package com.ap.sts.certificates.domain;

/**
 * A certificate/numbering business rule was not met. Carries a stable error code, a message
 * that says what went wrong and how to fix it (design system U8), the offending field (if any)
 * and the HTTP status to report (422 by default, 409 for concurrent-change conflicts).
 */
public class CertificateRuleException extends RuntimeException {

    public static final int UNPROCESSABLE = 422;
    public static final int CONFLICT = 409;

    private final String code;
    private final String field;
    private final int status;

    public CertificateRuleException(String code, String message) {
        this(code, message, null, UNPROCESSABLE);
    }

    public CertificateRuleException(String code, String message, String field) {
        this(code, message, field, UNPROCESSABLE);
    }

    public CertificateRuleException(String code, String message, String field, int status) {
        super(message);
        this.code = code;
        this.field = field;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String field() {
        return field;
    }

    public int status() {
        return status;
    }
}
