package com.ap.sts.shares;

import java.util.regex.Pattern;

/**
 * The naming convention for per-share-class certificate sequences (MDM-003, SL-002, TR-004).
 *
 * <p>This is the contract between S1 and S3. S1 creates both sequences when a share class is
 * defined; S3 draws numbers from them when printing. Two separate sequences per class, both
 * starting at 1:
 * <ul>
 *   <li><b>original</b> — numbers for newly issued certificates;</li>
 *   <li><b>replacement</b> — a deliberately separate series, so a replacement number can never
 *       collide with or reuse an original one (TR-004).</li>
 * </ul>
 *
 * <p>Sequence names are derived from the share-class id and validated against
 * {@link #SAFE_NAME} before they reach any SQL statement (SECURITY-05).
 */
public final class CertificateSequenceNames {

    /** Identifier-safe names only: lower-case letters, digits and underscores. */
    public static final Pattern SAFE_NAME = Pattern.compile("[a-z0-9_]{1,63}");

    private CertificateSequenceNames() {
    }

    /** e.g. {@code SC-000001 -> certificate_seq_sc_000001}. */
    public static String original(String shareClassId) {
        return validate("certificate_seq_" + slug(shareClassId));
    }

    /** e.g. {@code SC-000001 -> certificate_replacement_seq_sc_000001}. */
    public static String replacement(String shareClassId) {
        return validate("certificate_replacement_seq_" + slug(shareClassId));
    }

    private static String slug(String shareClassId) {
        if (shareClassId == null) {
            throw new IllegalArgumentException("shareClassId is required");
        }
        return shareClassId.trim().toLowerCase().replace('-', '_');
    }

    private static String validate(String name) {
        if (!SAFE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Unsafe sequence name derived from share class id");
        }
        return name;
    }
}
