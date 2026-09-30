package com.ap.sts.certificates.app;

import com.ap.sts.certificates.domain.Certificate;
import com.ap.sts.certificates.domain.CertificateRuleException;
import com.ap.sts.certificates.domain.CertificateStatus;
import com.ap.sts.certificates.persistence.CertificateRepository;
import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Certificate status changes — the ONLY way a certificate's life ends (no deletion, OI-19).
 * Not exposed over HTTP in the sprint (not in the frozen contract): {@link #postForTransaction}
 * is the in-process seam S2 calls when printing completes (TR-013) in Wave 2, inside its posting
 * transaction (TR-012). Void/replace/cancel flows are Deferred (TR-005/014); the rules are here.
 */
@Service
public class CertificateLifecycleService {

    private final CertificateRepository certificates;
    private final AuditService audit;

    public CertificateLifecycleService(CertificateRepository certificates, AuditService audit) {
        this.certificates = certificates;
        this.audit = audit;
    }

    @Transactional
    public Certificate changeStatus(long certificateId, CertificateStatus next, String reason) {
        Certificate certificate = certificates.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("Certificate " + certificateId + " was not found."));
        if (next != null && next.requiresReason() && (reason == null || reason.isBlank())) {
            throw new CertificateRuleException("reason_required",
                    "Give a reason for marking certificate " + certificate.getNumber() + " as " + next
                            + ". The certificate and its number are kept for the audit trail.", "reason");
        }
        Map<String, Object> before = CertificateSnapshots.of(certificate);
        certificate.transitionTo(next);
        Certificate saved = certificates.save(certificate);
        Map<String, Object> after = CertificateSnapshots.of(saved);
        if (reason != null && !reason.isBlank()) {
            after.put("reason", reason.trim());
        }
        audit.record("certificate.status-change", CertificateSnapshots.ref(saved), before, after);
        return saved;
    }

    /** TR-013: printing is complete, so the transaction's Issued certificates become Posted. Idempotent. */
    @Transactional
    public List<Certificate> postForTransaction(String transactionId) {
        List<Certificate> all = certificates.findByTransactionIdOrderByLineNo(transactionId);
        if (all.isEmpty()) {
            throw new CertificateRuleException("not_printed",
                    "Transaction " + transactionId + " has no printed certificates yet. Print them before "
                            + "marking the transaction as printed (TR-013).", "transactionId");
        }
        for (Certificate certificate : all) {
            if (certificate.getStatus() == CertificateStatus.Issued) {
                Map<String, Object> before = CertificateSnapshots.of(certificate);
                certificate.transitionTo(CertificateStatus.Posted);
                Certificate saved = certificates.save(certificate);
                audit.record("certificate.posted", CertificateSnapshots.ref(saved), before, CertificateSnapshots.of(saved));
            }
        }
        return all;
    }
}
