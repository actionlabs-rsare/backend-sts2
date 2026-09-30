package com.ap.sts.stockholders;

/**
 * Masking helpers for personal data (SECURITY-03/13, security-nfr.md T7).
 * TIN, nationality, gender, mobile, address and email are personal information under the
 * PH Data Privacy Act — they are stored, but they must never reach a log line in the clear.
 *
 * <p>Rule for this unit: log the Stockholder Code, never the person. Where a value genuinely
 * has to appear (support triage), pass it through one of these methods first.
 */
public final class Pii {

    private static final String REDACTED = "***";

    private Pii() {
    }

    /**
     * Keeps only the last four characters of a TIN, e.g. {@code 123-456-789-000 -> ***-000}.
     * Returns {@code null} for null so callers can log an absent value as absent.
     */
    public static String maskTin(String tin) {
        if (tin == null) {
            return null;
        }
        String trimmed = tin.trim();
        if (trimmed.length() <= 4) {
            return REDACTED;
        }
        // The tail often starts with the TIN's own separator ("...-000"), so no extra one is added.
        return REDACTED + trimmed.substring(trimmed.length() - 4);
    }

    /** Keeps the first character of the local part and the domain, e.g. {@code m***@example.com}. */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return REDACTED;
        }
        return email.charAt(0) + REDACTED + email.substring(at);
    }

    /** Keeps the last three characters of a mobile number, e.g. {@code ***123}. */
    public static String maskMobile(String mobile) {
        if (mobile == null) {
            return null;
        }
        String trimmed = mobile.trim();
        if (trimmed.length() <= 3) {
            return REDACTED;
        }
        return REDACTED + trimmed.substring(trimmed.length() - 3);
    }

    /** Free-text personal data (address) is never partially revealed. */
    public static String redact(String value) {
        return value == null ? null : REDACTED;
    }
}
