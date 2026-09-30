package com.ap.sts.transactions;

/**
 * Business-rule validation failure for a transfer (e.g. shares exceed the transferor's holding,
 * or additional subscriptions attempted here — TR-003). Maps to HTTP 422 via a controller-local
 * handler in {@link TransactionController} (the shared advice only knows bean-validation 422s).
 */
public class TransferValidationException extends RuntimeException {
    public TransferValidationException(String message) {
        super(message);
    }
}
