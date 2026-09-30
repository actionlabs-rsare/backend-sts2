package com.ap.sts.transactions;

/** Kind of transfer (TR-003), matching transactions.openapi.yaml #/schemas/TransferType. */
public enum TransferType {
    Sale,
    Assignment,
    Donation,
    ChangeOfNominee
}
