package com.ap.sts.certificates.app;

import com.ap.sts.certificates.domain.Certificate;
import com.ap.sts.certificates.domain.CertificateEvents.CertificateNumberAssigned;
import com.ap.sts.certificates.domain.CertificateEvents.CertificatePrinted;
import com.ap.sts.certificates.domain.CertificateOrigin;
import com.ap.sts.certificates.domain.CertificateRuleException;
import com.ap.sts.certificates.domain.NumberingMode;
import com.ap.sts.certificates.domain.PrintMode;
import com.ap.sts.certificates.persistence.CertificateRepository;
import com.ap.sts.certificates.persistence.SequenceGateway;
import com.ap.sts.certificates.ports.CertificatePorts.CompanyDirectory;
import com.ap.sts.certificates.ports.CertificatePorts.CompanyProfile;
import com.ap.sts.certificates.ports.CertificatePorts.PrintableLine;
import com.ap.sts.certificates.ports.CertificatePorts.PrintableTransaction;
import com.ap.sts.certificates.ports.CertificatePorts.ShareClassDirectory;
import com.ap.sts.certificates.ports.CertificatePorts.ShareClassInfo;
import com.ap.sts.certificates.ports.CertificatePorts.StockholderDirectory;
import com.ap.sts.certificates.ports.CertificatePorts.StockholderRef;
import com.ap.sts.certificates.ports.CertificatePorts.TransactionSource;
import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Issues the certificates of an approved transaction in ONE database transaction (TR-017/TR-018):
 * each line takes the next gapless number of its share class and becomes an 'Issued' certificate
 * carrying its printed face (the 10 ticked fields), and every assignment is audited. Everything is
 * validated before any number is taken; if anything still fails, the rollback returns the numbers.
 * Authorisation of manual numbers happens before this (CertificatePrintService).
 */
@Service
public class CertificateIssuer {

    /** Certificates are dated in Philippine time. */
    static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    private final CertificateRepository certificates;
    private final NumberingService numbering;
    private final SequenceGateway gateway;
    private final TransactionSource transactions;
    private final ShareClassDirectory shareClasses;
    private final CompanyDirectory companies;
    private final StockholderDirectory stockholders;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public CertificateIssuer(CertificateRepository certificates, NumberingService numbering, SequenceGateway gateway,
                             TransactionSource transactions, ShareClassDirectory shareClasses,
                             CompanyDirectory companies, StockholderDirectory stockholders, AuditService audit,
                             ApplicationEventPublisher events) {
        this.certificates = certificates;
        this.numbering = numbering;
        this.gateway = gateway;
        this.transactions = transactions;
        this.shareClasses = shareClasses;
        this.companies = companies;
        this.stockholders = stockholders;
        this.audit = audit;
        this.events = events;
    }

    /**
     * @param manualNumber normalised Team-Leader-approved number that must equal the next number and confirms the
     *                     first certificate of the print (the rest follow on), or null for automatic numbering
     * @param approvedBy   the Team Leader who approved {@code manualNumber}; null when automatic
     */
    @Transactional
    public List<Certificate> issue(String transactionId, PrintMode mode, String manualNumber, String approvedBy) {
        gateway.lockTransactionForPrint(transactionId);

        List<Certificate> existing = certificates.findByTransactionIdOrderByLineNo(transactionId);
        if (!existing.isEmpty()) {
            return reprint(transactionId, existing, mode, manualNumber);
        }

        PrintableTransaction tx = transactions.find(transactionId).orElseThrow(() -> new NotFoundException(
                "Transaction " + transactionId + " was not found. Check the reference number."));
        if (!tx.isApproved()) {
            throw new CertificateRuleException("not_printable",
                    "Transaction " + transactionId + " is " + readable(tx.status()) + ". Only approved transactions "
                            + "can be printed (TR-017); complete the approval first.", "transactionId");
        }
        if (tx.lines().isEmpty()) {
            throw new CertificateRuleException("no_lines",
                    "Transaction " + transactionId + " has no transferee lines, so there is nothing to print.",
                    "transactionId");
        }
        // Validate everything first: President + Corporate Secretary are printed in both modes (ticked).
        CompanyProfile company = requireOfficers(tx.companyCode());
        List<ShareClassInfo> lineClasses = new ArrayList<>();
        List<String> holderNames = new ArrayList<>();
        for (PrintableLine line : tx.lines()) {
            lineClasses.add(requireShareClass(tx, line));
            holderNames.add(requireCorporateHolder(tx, line));
        }
        // S3-Q2: a class in Manual mode (pre-printed stock) needs the Team Leader to confirm the number on
        // the first pre-printed form. The manual number confirms the first certificate, so the first line's
        // class decides.
        ShareClassInfo firstClass = lineClasses.get(0);
        if (manualNumber == null && numbering.modeOf(firstClass.id()) == NumberingMode.Manual) {
            throw new CertificateRuleException("manual_number_required",
                    firstClass.stockType() + " uses manual numbering for pre-printed stock (S3-Q2): a Team Leader "
                            + "must enter and approve the number on the first pre-printed form. The next number is "
                            + numbering.nextOriginal(firstClass.id()) + ".", "manualNumber");
        }

        String actor = Actors.current();
        Instant now = Instant.now();
        LocalDate issuedOn = LocalDate.now(MANILA);
        List<Certificate> issued = new ArrayList<>();
        int lineNo = 0;
        for (PrintableLine line : tx.lines()) {
            ShareClassInfo shareClass = lineClasses.get(lineNo);
            String holderName = holderNames.get(lineNo);
            lineNo++;
            // Every certificate takes the next number from the gapless counter (row-locked until commit).
            String number = numbering.drawNext(shareClass.id(), CertificateOrigin.original);
            // S3-Q3/S3-Q8: a manual number confirms the FIRST certificate of the print (e.g. the number on
            // the first pre-printed form); the others follow on. It must be the next number, or it would
            // leave a gap. Throwing rolls the counter back, so nothing is consumed.
            boolean confirmsThisOne = manualNumber != null && lineNo == 1;
            if (confirmsThisOne && !manualNumber.equals(number)) {
                throw new CertificateRuleException("manual_number_not_next",
                        "Manual number " + manualNumber + " is not the next " + shareClass.stockType()
                                + " number. Numbers must run on without gaps (S3-Q8), and the next one is " + number
                                + ". Use " + number + ", or print without a manual number."
                                + (tx.lines().size() > 1 ? " The other certificates of this print follow on from it." : ""),
                        "manualNumber");
            }
            Certificate certificate = certificates.save(Certificate.builder()
                    .number(number)
                    .shareClassId(shareClass.id())
                    .companyCode(tx.companyCode())
                    .holderStockholderCode(line.toStockholderCode())
                    .shares(line.shares())
                    .origin(CertificateOrigin.original)
                    .transactionId(transactionId)
                    .lineNo(lineNo)
                    .printMode(mode)
                    .manuallyApprovedBy(confirmsThisOne ? approvedBy : null)
                    .issuedOn(issuedOn)
                    .stockType(shareClass.stockType())
                    .companyName(company.name())
                    .holderName(holderName)
                    .parValue(shareClass.parValue())
                    .presidentName(company.presidentName())
                    .corporateSecretaryName(company.corporateSecretaryName())
                    .printedAt(now)
                    .printedBy(actor)
                    .build());
            audit.record("certificate.number-assigned", CertificateSnapshots.ref(certificate), null,
                    CertificateSnapshots.of(certificate));
            events.publishEvent(new CertificateNumberAssigned(transactionId, shareClass.id(), number,
                    CertificateOrigin.original, confirmsThisOne, now));
            issued.add(certificate);
        }

        List<String> numbers = issued.stream().map(Certificate::getNumber).toList();
        Map<String, Object> summary = printSummary(mode, numbers, false);
        if (manualNumber != null) {
            summary.put("manualStart", manualNumber);
            summary.put("approvedBy", approvedBy);
        }
        audit.record("certificate.print", "transaction/" + transactionId, null, summary);
        events.publishEvent(new CertificatePrinted(transactionId, numbers, mode, false, now));
        return issued;
    }

    private List<Certificate> reprint(String transactionId, List<Certificate> existing, PrintMode mode,
                                      String manualNumber) {
        List<String> numbers = existing.stream().map(Certificate::getNumber).toList();
        if (manualNumber != null) {
            throw new CertificateRuleException("already_printed",
                    "Transaction " + transactionId + " already has certificate(s) " + String.join(", ", numbers)
                            + ". A reprint keeps the same numbers; changing a printed number needs a void and "
                            + "reissue (TR-014, not in this sprint).", "manualNumber");
        }
        audit.record("certificate.reprint", "transaction/" + transactionId, null, printSummary(mode, numbers, true));
        events.publishEvent(new CertificatePrinted(transactionId, numbers, mode, true, Instant.now()));
        return existing;
    }

    private ShareClassInfo requireShareClass(PrintableTransaction tx, PrintableLine line) {
        ShareClassInfo shareClass = shareClasses.find(line.shareClassId()).orElseThrow(() ->
                new CertificateRuleException("unknown_share_class",
                        "Share class " + line.shareClassId() + " on " + tx.id() + " was not found. "
                                + "Check the company's share classes (MDM-003).", "transactionId"));
        if (!shareClass.companyCode().equals(tx.companyCode())) {
            throw new CertificateRuleException("share_class_company_mismatch",
                    "Share class " + shareClass.id() + " belongs to " + shareClass.companyCode() + ", not "
                            + tx.companyCode() + ". Correct the transaction before printing.", "transactionId");
        }
        if (shareClass.parValue() == null || shareClass.stockType() == null || shareClass.stockType().isBlank()) {
            throw new CertificateRuleException("share_class_incomplete",
                    "Share class " + shareClass.id() + " has no stock type or par value, and both are printed on the "
                            + "certificate. Complete it in Maintenance › Company shares (MDM-003).", "transactionId");
        }
        return shareClass;
    }

    private CompanyProfile requireOfficers(String companyCode) {
        CompanyProfile company = companies.find(companyCode).orElse(null);
        if (company == null || !company.hasOfficers()) {
            throw new CertificateRuleException("officers_missing",
                    "Company " + companyCode + " has no President or Corporate Secretary on file. Both names are "
                            + "printed on every certificate (TR-017), so add them in Maintenance › Companies first.",
                    "transactionId");
        }
        return company;
    }

    /** S3-Q9: the holder printed on a certificate is always a company; individuals are refused. */
    private String requireCorporateHolder(PrintableTransaction tx, PrintableLine line) {
        StockholderRef holder = stockholders.find(line.toStockholderCode())
                .filter(h -> h.name() != null && !h.name().isBlank())
                .orElseThrow(() -> new CertificateRuleException("unknown_stockholder",
                        "Stockholder " + line.toStockholderCode() + " on " + tx.id() + " was not found, so the "
                                + "certificate can't show the holder's name. Check the stockholder (MDM-002).",
                        "transactionId"));
        if (!holder.isCorporate()) {
            throw new CertificateRuleException("individual_holder_not_allowed",
                    "Stockholder " + holder.code() + " on " + tx.id() + " is an individual. Certificates carry company "
                            + "names only (S3-Q9), so this one can't be printed. Check the transferee (MDM-002).",
                    "transactionId");
        }
        return holder.name();
    }

    /** "PendingFirstApproval" → "pending first approval" for user-facing messages (U2). */
    static String readable(String status) {
        if (status == null || status.isBlank()) {
            return "not approved";
        }
        return status.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(java.util.Locale.ROOT);
    }

    private static Map<String, Object> printSummary(PrintMode mode, List<String> numbers, boolean reprint) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", mode.name());
        m.put("numbers", numbers);
        m.put("reprint", reprint);
        return m;
    }
}
