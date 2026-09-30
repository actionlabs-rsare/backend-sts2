package com.ap.sts.shared.error;

/** Access denied (deny-by-default; SL-005). Maps to HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
