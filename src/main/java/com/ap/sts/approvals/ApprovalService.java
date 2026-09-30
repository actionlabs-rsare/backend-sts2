package com.ap.sts.approvals;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.auth.CurrentUser;
import com.ap.sts.shared.auth.Role;
import com.ap.sts.shared.auth.Session;
import com.ap.sts.shared.error.ForbiddenException;
import com.ap.sts.transactions.Transaction;
import com.ap.sts.transactions.TransactionDtos.ApprovalStepView;
import com.ap.sts.transactions.TransactionRepository;
import com.ap.sts.transactions.TransactionStatus;
import com.ap.sts.transactions.TransferValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Approval Workflow state machine (SL-004, refined OI-16). Server-side and role-checked per
 * transition (T10) — the UI is never the gate:
 *
 * <ul>
 *   <li><b>submit</b>: {@code Submitted → PendingFirstApproval} (routes to a 1st Level Approver);</li>
 *   <li><b>approve</b> level 1: a <b>standard</b> transfer becomes {@code Approved} (posts after the
 *       first approval); an <b>override/void</b> transfer goes to {@code PendingSecondApproval}
 *       (routed to the Team Leader);</li>
 *   <li><b>approve</b> level 2: {@code Approved};</li>
 *   <li><b>reject</b>: {@code Rejected} from any pending step (reason mandatory — T6).</li>
 * </ul>
 *
 * Actual posting happens on print completion (TR-013), handled by the Transactions context.
 */
@Service
public class ApprovalService {

    private static final Set<Role> LEVEL_1_ROLES = EnumSet.of(Role.FirstLevelApprover, Role.Admin);
    private static final Set<Role> LEVEL_2_ROLES = EnumSet.of(Role.SecondLevelApprover, Role.TeamLeader, Role.Admin);

    private final TransactionRepository transactions;
    private final ApprovalRequestRepository approvals;
    private final AuditService audit;

    public ApprovalService(TransactionRepository transactions, ApprovalRequestRepository approvals,
                           AuditService audit) {
        this.transactions = transactions;
        this.approvals = approvals;
        this.audit = audit;
    }

    /** {@code Submitted → PendingFirstApproval}; opens the level-1 approval step. */
    @Transactional
    public Transaction submit(String id) {
        Transaction tx = require(id);
        if (tx.getStatus() != TransactionStatus.Submitted) {
            throw new TransferValidationException(
                    "Only a Submitted transaction can be sent for approval (current: " + tx.getStatus() + ").");
        }
        approvals.save(new ApprovalRequest(id, 1, Role.FirstLevelApprover));
        TransactionStatus before = tx.getStatus();
        tx.setStatus(TransactionStatus.PendingFirstApproval);
        Transaction saved = transactions.save(tx);
        audit.record("transaction.submit", ref(id), snapshot(before), snapshot(saved.getStatus()));
        return saved;
    }

    /** Approves the current pending step for the acting session; advances the transaction. */
    @Transactional
    public Transaction approve(String id, String reason) {
        Transaction tx = require(id);
        ApprovalRequest step = currentStep(id);
        Session actor = authorize(step);
        if (tx.isException() && (reason == null || reason.isBlank())) {
            throw new TransferValidationException("A reason is required to approve an override/void transaction.");
        }
        step.decide(ApprovalRequest.Decision.Approved, actor.userId(), reason);
        approvals.save(step);

        TransactionStatus before = tx.getStatus();
        if (step.getLevel() == 1 && tx.isException()) {
            // Override/void: escalate to the Team Leader for a second approval.
            approvals.save(new ApprovalRequest(id, 2, Role.TeamLeader));
            tx.setStatus(TransactionStatus.PendingSecondApproval);
        } else {
            // Standard first approval, or the second approval of an exception → fully approved.
            tx.setStatus(TransactionStatus.Approved);
        }
        Transaction saved = transactions.save(tx);
        audit.record("transaction.approve", ref(id), snapshot(before), snapshot(saved.getStatus()));
        return saved;
    }

    /** Rejects the current pending step (reason mandatory — T6); ends the workflow. */
    @Transactional
    public Transaction reject(String id, String reason) {
        Transaction tx = require(id);
        ApprovalRequest step = currentStep(id);
        Session actor = authorize(step);
        if (reason == null || reason.isBlank()) {
            throw new TransferValidationException("A reason is required to reject a transaction.");
        }
        step.decide(ApprovalRequest.Decision.Rejected, actor.userId(), reason);
        approvals.save(step);

        TransactionStatus before = tx.getStatus();
        tx.setStatus(TransactionStatus.Rejected);
        Transaction saved = transactions.save(tx);
        audit.record("transaction.reject", ref(id), snapshot(before), snapshot(saved.getStatus()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ApprovalStepView> stepsFor(String id) {
        return approvals.findByTransactionIdOrderByLevelAsc(id).stream()
                .map(a -> new ApprovalStepView(a.getLevel(), a.getRequiredRole().name(),
                        a.getDecision().name(), a.getActor(), a.getReason(), a.getDecidedAt()))
                .toList();
    }

    private ApprovalRequest currentStep(String id) {
        return approvals.findFirstByTransactionIdAndDecisionOrderByLevelAsc(id, ApprovalRequest.Decision.Pending)
                .orElseThrow(() -> new TransferValidationException(
                        "No approval step is pending for this transaction."));
    }

    /** Deny-by-default per-transition role check (T10). Returns the acting session when allowed. */
    private Session authorize(ApprovalRequest step) {
        Session session = CurrentUser.get();
        if (session == null) {
            throw new ForbiddenException("Authentication required");
        }
        Set<Role> allowed = step.getLevel() == 1 ? LEVEL_1_ROLES : LEVEL_2_ROLES;
        if (!allowed.contains(session.role())) {
            throw new ForbiddenException(
                    "Role " + session.role() + " may not act on approval level " + step.getLevel() + ".");
        }
        return session;
    }

    private Transaction require(String id) {
        return transactions.findById(id)
                .orElseThrow(() -> new com.ap.sts.shared.error.NotFoundException("Transaction not found: " + id));
    }

    private static String ref(String id) {
        return "transaction/" + id;
    }

    private static Map<String, Object> snapshot(TransactionStatus status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status.name());
        return m;
    }
}
