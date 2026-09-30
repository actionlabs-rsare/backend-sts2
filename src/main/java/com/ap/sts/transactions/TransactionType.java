package com.ap.sts.transactions;

/**
 * Transaction type, matching transactions.openapi.yaml #/schemas/TransactionType.
 * S2 implements {@link #Transfer} this sprint (TR-003); the other values are
 * modelled for the shared status set only (OI-16).
 */
public enum TransactionType {
    Transfer,
    Subscription,
    DirectIssue,
    Redemption,
    Amendment,
    Replacement,
    Void
}
