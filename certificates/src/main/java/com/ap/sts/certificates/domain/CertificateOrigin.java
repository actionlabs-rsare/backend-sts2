package com.ap.sts.certificates.domain;

/**
 * Which numbering sequence a certificate came from (contract Certificate.origin).
 * Replacement numbers use a sequence SEPARATE from original issuances (TR-004).
 * Constant names match the contract values and the DB check constraint.
 */
public enum CertificateOrigin {
    original(""),
    replacement("R-");

    private final String prefix;

    CertificateOrigin(String prefix) {
        this.prefix = prefix;
    }

    /** Display prefix of numbers in this series: "" for originals, "R-" for replacements (S3-Q1). */
    public String prefix() {
        return prefix;
    }
}
