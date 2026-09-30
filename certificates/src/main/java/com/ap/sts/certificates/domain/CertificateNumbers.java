package com.ap.sts.certificates.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Certificate number formatting and parsing. Originals print unpadded, as on AP's real certificates
 * ("1", "235"; S3-Q1 answered by the ticked sample, 2026-09-29). Replacements carry an "R-" prefix
 * ("R-7") so the separate series can't collide with originals in the same class. Leading zeros are
 * accepted on input and dropped. Sequence values and manual numbers share this format, so a manual
 * number can be matched against the counter and skipped when the counter reaches it.
 */
public final class CertificateNumbers {

    public static final long MAX_NUMBER = 9_999_999_999L;
    private static final Pattern DIGITS = Pattern.compile("^\\d{1,10}$");
    private static final Pattern REPLACEMENT = Pattern.compile("^(?:[Rr]-?)?(\\d{1,10})$");

    private CertificateNumbers() {
    }

    public static String format(CertificateOrigin origin, long value) {
        if (value < 1 || value > MAX_NUMBER) {
            throw new IllegalArgumentException("certificate number out of range: " + value);
        }
        return origin.prefix() + value;
    }

    /**
     * Parses a starting point typed by an administrator. Originals accept digits ("12", "0012");
     * replacements also accept the "R-" prefix ("R-12", "R12").
     */
    public static long parse(CertificateOrigin origin, String text, String field) {
        String value = text == null ? "" : text.trim();
        Matcher m = (origin == CertificateOrigin.replacement ? REPLACEMENT : DIGITS).matcher(value);
        if (!m.matches()) {
            String example = origin == CertificateOrigin.replacement ? "R-1" : "1";
            throw new CertificateRuleException("validation_error",
                    "'" + value + "' is not a valid certificate number. Use digits only, for example " + example + ".",
                    field);
        }
        long parsed = Long.parseLong(origin == CertificateOrigin.replacement ? m.group(1) : value);
        if (parsed < 1) {
            throw new CertificateRuleException("validation_error",
                    "Certificate numbers start at 1. Enter 1 or higher.", field);
        }
        return parsed;
    }

    /** Normalises a Team-Leader-approved manual number into the original series format. */
    public static String normalizeManual(String text) {
        long n = parse(CertificateOrigin.original, text, "manualNumber");
        return format(CertificateOrigin.original, n);
    }
}
