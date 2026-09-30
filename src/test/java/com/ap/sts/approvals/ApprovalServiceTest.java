package com.ap.sts.approvals;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.auth.CurrentUser;
import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.Role;
import com.ap.sts.shared.auth.Session;
import com.ap.sts.shared.error.ForbiddenException;
import com.ap.sts.transactions.Transaction;
import com.ap.sts.transactions.TransactionRepository;
import com.ap.sts.transactions.TransactionStatus;
import com.ap.sts.transactions.TransactionType;
import com.ap.sts.transactions.TransferType;
import com.ap.sts.transactions.TransferValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalServiceTest {

    private static final String ID = "TRF-000001";

    @Mock
    private TransactionRepository transactions;
    @Mock
    private ApprovalRequestRepository approvals;
    @Mock
    private AuditService audit;

    @InjectMocks
    private ApprovalService service;

    @AfterEach
    void clearSession() {
        CurrentUser.clear();
    }

    private Transaction tx(TransactionStatus status, boolean exception) {
        Transaction t = new Transaction(ID, TransactionType.Transfer, TransferType.Sale,
                "CO-001", LocalDate.of(2026, 9, 29), exception);
        t.setStatus(status);
        when(transactions.findById(ID)).thenReturn(Optional.of(t));
        when(transactions.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        return t;
    }

    private void signedInAs(Role role) {
        CurrentUser.set(new Session("dev-" + role, role + " (dev)", role, EnumSet.allOf(Permission.class)));
    }

    private void pendingStep(int level, Role required) {
        when(approvals.findFirstByTransactionIdAndDecisionOrderByLevelAsc(ID, ApprovalRequest.Decision.Pending))
                .thenReturn(Optional.of(new ApprovalRequest(ID, level, required)));
        when(approvals.save(any(ApprovalRequest.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void submitMovesToPendingFirstApprovalAndOpensLevel1() {
        Transaction t = tx(TransactionStatus.Submitted, false);
        when(approvals.save(any(ApprovalRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        service.submit(ID);

        assertEquals(TransactionStatus.PendingFirstApproval, t.getStatus());
        ArgumentCaptor<ApprovalRequest> cap = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(approvals).save(cap.capture());
        assertEquals(1, cap.getValue().getLevel());
        assertEquals(Role.FirstLevelApprover, cap.getValue().getRequiredRole());
    }

    @Test
    void submitOnlyFromSubmitted() {
        tx(TransactionStatus.Approved, false);
        assertThrows(TransferValidationException.class, () -> service.submit(ID));
    }

    @Test
    void standardTransferIsApprovedAfterFirstApproval() {
        // SL-004: a standard transfer posts after the first approval → Approved, no second step.
        Transaction t = tx(TransactionStatus.PendingFirstApproval, false);
        pendingStep(1, Role.FirstLevelApprover);
        signedInAs(Role.FirstLevelApprover);

        service.approve(ID, null);

        assertEquals(TransactionStatus.Approved, t.getStatus());
        verify(audit).record(eq("transaction.approve"), eq("transaction/" + ID), any(), any());
    }

    @Test
    void overrideRequiresSecondApprovalByTeamLeader() {
        // SL-004 (OI-16): override/void escalates to PendingSecondApproval, then the Team Leader approves.
        Transaction t = tx(TransactionStatus.PendingFirstApproval, true);
        pendingStep(1, Role.FirstLevelApprover);
        signedInAs(Role.FirstLevelApprover);

        service.approve(ID, "override: correcting a mis-posted certificate");
        assertEquals(TransactionStatus.PendingSecondApproval, t.getStatus());

        ArgumentCaptor<ApprovalRequest> cap = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(approvals, atLeastOnce()).save(cap.capture());
        assertTrue(cap.getAllValues().stream()
                        .anyMatch(a -> a.getLevel() == 2 && a.getRequiredRole() == Role.TeamLeader),
                "a level-2 Team Leader step must be opened for an exception");

        // Second approval by the Team Leader completes it.
        pendingStep(2, Role.TeamLeader);
        signedInAs(Role.TeamLeader);
        service.approve(ID, "endorsed");
        assertEquals(TransactionStatus.Approved, t.getStatus());
    }

    @Test
    void approvalIsRoleCheckedPerTransition() {
        // T10: a 2nd Level Approver may not act on the level-1 step (wrong role) → 403.
        tx(TransactionStatus.PendingFirstApproval, false);
        pendingStep(1, Role.FirstLevelApprover);
        signedInAs(Role.SecondLevelApprover);
        assertThrows(ForbiddenException.class, () -> service.approve(ID, null));
    }

    @Test
    void exceptionApprovalRequiresReason() {
        tx(TransactionStatus.PendingFirstApproval, true);
        pendingStep(1, Role.FirstLevelApprover);
        signedInAs(Role.FirstLevelApprover);
        assertThrows(TransferValidationException.class, () -> service.approve(ID, "  "));
    }

    @Test
    void rejectRequiresReasonAndEndsWorkflow() {
        Transaction t = tx(TransactionStatus.PendingFirstApproval, false);
        pendingStep(1, Role.FirstLevelApprover);
        signedInAs(Role.FirstLevelApprover);

        assertThrows(TransferValidationException.class, () -> service.reject(ID, null));

        service.reject(ID, "documents incomplete");
        assertEquals(TransactionStatus.Rejected, t.getStatus());
        verify(audit).record(eq("transaction.reject"), eq("transaction/" + ID), any(), any());
    }

    @Test
    void approveWithNoPendingStepFails() {
        tx(TransactionStatus.Approved, false);
        when(approvals.findFirstByTransactionIdAndDecisionOrderByLevelAsc(ID, ApprovalRequest.Decision.Pending))
                .thenReturn(Optional.empty());
        signedInAs(Role.FirstLevelApprover);
        assertThrows(TransferValidationException.class, () -> service.approve(ID, null));
    }

    @Test
    void unauthenticatedApprovalIsForbidden() {
        tx(TransactionStatus.PendingFirstApproval, false);
        pendingStep(1, Role.FirstLevelApprover);
        CurrentUser.clear();
        assertThrows(ForbiddenException.class, () -> service.approve(ID, null));
    }

    @Test
    void stepsForMapsDecidedRequests() {
        // Covers the ApprovalStepView projection and the decided-request getters (audit history view).
        ApprovalRequest decided = new ApprovalRequest(ID, 1, Role.FirstLevelApprover);
        decided.decide(ApprovalRequest.Decision.Approved, "dev-firstlevelapprover", "endorsed");
        when(approvals.findByTransactionIdOrderByLevelAsc(ID)).thenReturn(java.util.List.of(decided));

        var steps = service.stepsFor(ID);

        assertEquals(1, steps.size());
        assertEquals(1, steps.get(0).level());
        assertEquals("FirstLevelApprover", steps.get(0).requiredRole());
        assertEquals("Approved", steps.get(0).decision());
        assertEquals("dev-firstlevelapprover", steps.get(0).actor());
        assertEquals("endorsed", steps.get(0).reason());
        assertTrue(decided.getCreatedAt() != null && decided.getTransactionId().equals(ID));
    }
}
