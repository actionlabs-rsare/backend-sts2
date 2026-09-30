package com.ap.sts.certificates.app;

import com.ap.sts.certificates.domain.Certificate;
import com.ap.sts.certificates.domain.CertificateNumbers;
import com.ap.sts.certificates.domain.PrintMode;
import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.auth.AccessMatrix;
import com.ap.sts.shared.auth.CurrentUser;
import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.Session;
import com.ap.sts.shared.error.ForbiddenException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point for TR-017/TR-018 printing. Enforces the Team Leader rule for manual numbers
 * SERVER-SIDE (security-nfr T11): a manual number is accepted only when the request says it is
 * Team-Leader-approved AND the signed-in role actually holds certificate:approve. A client flag
 * alone is never enough. The number itself must then be the next in sequence (S3-Q8, checked by
 * CertificateIssuer inside the print transaction). Denied attempts are audited outside the print transaction, so the
 * audit entry survives the rejection.
 */
@Service
public class CertificatePrintService {

    public static final String MODULE = "certificate";

    private final CertificateIssuer issuer;
    private final AccessMatrix accessMatrix;
    private final AuditService audit;

    public CertificatePrintService(CertificateIssuer issuer, AccessMatrix accessMatrix, AuditService audit) {
        this.issuer = issuer;
        this.accessMatrix = accessMatrix;
        this.audit = audit;
    }

    public record PrintCommand(String transactionId, PrintMode mode, String manualNumber, boolean teamLeaderApproval) {
    }

    public List<Certificate> print(PrintCommand command) {
        String manual = command.manualNumber() == null || command.manualNumber().isBlank()
                ? null : command.manualNumber().trim();
        if (manual == null) {
            return issuer.issue(command.transactionId(), command.mode(), null, null);
        }

        Session session = CurrentUser.get();
        boolean roleCanApprove = session != null
                && accessMatrix.allows(session.role(), MODULE, Permission.approve);
        if (!command.teamLeaderApproval() || !roleCanApprove) {
            Map<String, Object> attempt = new LinkedHashMap<>();
            attempt.put("manualNumber", manual);
            attempt.put("role", session == null ? null : session.role().name());
            attempt.put("teamLeaderApproval", command.teamLeaderApproval());
            audit.record("certificate.manual-number.denied", "transaction/" + command.transactionId(), null, attempt);
            throw new ForbiddenException(roleCanApprove
                    ? "Manual certificate numbers need Team Leader approval (TR-018). Confirm the approval, "
                    + "or print without a manual number."
                    : "Only a Team Leader can approve a manual certificate number (TR-018). Print without a "
                    + "manual number, or ask a Team Leader.");
        }
        String normalized = CertificateNumbers.normalizeManual(manual);
        return issuer.issue(command.transactionId(), command.mode(), normalized, session.userId());
    }
}
