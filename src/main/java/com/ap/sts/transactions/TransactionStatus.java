package com.ap.sts.transactions;

/**
 * Transaction status set, refined per OI-16 (Gate 3). No Draft — a transaction starts at
 * {@link #Submitted}. {@link #PendingSecondApproval} is reached only by override/void
 * (exception) transactions. Lifecycle:
 *
 * <pre>
 *   Submitted → PendingFirstApproval → [PendingSecondApproval: override/void only] → Approved → Posted
 *                                    ↘ Rejected (from any pending step)
 * </pre>
 *
 * Standard transfers post after the first approval (SL-004); posting itself is triggered by
 * print completion (TR-013) and is atomic with the holdings update (TR-012).
 */
public enum TransactionStatus {
    Submitted,
    PendingFirstApproval,
    PendingSecondApproval,
    Approved,
    Posted,
    Rejected
}
