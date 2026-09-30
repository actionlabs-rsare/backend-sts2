package com.ap.sts.shares;

/**
 * A share-class rule was broken: a duplicate stock type for the company, or share counts that do
 * not nest (treasury ≤ issued ≤ subscribed ≤ authorized).
 *
 * <p>Mapped to 422 by {@link ShareClassExceptionHandler} so the client gets the same
 * validation-error shape as bean-validation failures rather than a generic 500 (SECURITY-15).
 */
public class ShareClassConflictException extends RuntimeException {

    public ShareClassConflictException(String message) {
        super(message);
    }
}
