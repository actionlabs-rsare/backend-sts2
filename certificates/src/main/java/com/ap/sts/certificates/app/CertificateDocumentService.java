package com.ap.sts.certificates.app;

import com.ap.sts.certificates.domain.Certificate;
import com.ap.sts.certificates.domain.PrintMode;
import com.ap.sts.certificates.pdf.CertificatePdfRenderer;
import com.ap.sts.certificates.pdf.CertificateSheet;
import com.ap.sts.certificates.persistence.CertificateRepository;
import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the printable PDF for the certificates a print assigned (TR-017 output). Renders from the
 * face saved on each certificate at print time — never from live master data — so a reprint shows
 * exactly what was issued, even if an officer or the holder's record changed since.
 */
@Service
public class CertificateDocumentService {

    private final CertificateRepository certificates;
    private final CertificatePdfRenderer renderer;
    private final AuditService audit;

    public CertificateDocumentService(CertificateRepository certificates, CertificatePdfRenderer renderer,
                                      AuditService audit) {
        this.certificates = certificates;
        this.renderer = renderer;
        this.audit = audit;
    }

    public record CertificateDocument(String fileName, PrintMode mode, byte[] content) {
    }

    /** @param modeOverride null = the mode chosen at print time */
    @Transactional
    public CertificateDocument render(String transactionId, PrintMode modeOverride) {
        List<Certificate> printed = certificates.findByTransactionIdOrderByLineNo(transactionId);
        if (printed.isEmpty()) {
            throw new NotFoundException("No certificates have been printed for transaction " + transactionId
                    + " yet. Print them first, then download the PDF.");
        }
        PrintMode mode = modeOverride != null ? modeOverride : printed.get(0).getPrintMode();
        List<CertificateSheet> sheets = printed.stream().map(CertificateDocumentService::sheet).toList();
        byte[] pdf = renderer.render(sheets, mode);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("mode", mode.name());
        after.put("numbers", printed.stream().map(Certificate::getNumber).toList());
        after.put("pages", sheets.size());
        audit.record("certificate.document", "transaction/" + transactionId, null, after);
        return new CertificateDocument("certificates-" + transactionId + "-" + mode.name() + ".pdf", mode, pdf);
    }

    static CertificateSheet sheet(Certificate c) {
        return new CertificateSheet(
                c.getNumber(),
                c.getOrigin(),
                c.getStockType(),
                c.getShares(),
                c.getCompanyName(),
                c.getHolderName(),
                c.getIssuedOn(),
                c.getPresidentName(),
                c.getCorporateSecretaryName(),
                c.getParValue(),
                c.getTransactionId());
    }
}
