package com.ap.sts.shares;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The S1 ↔ S3 naming convention for certificate sequences (MDM-003, SL-002, TR-004).
 * S3 derives the same names, so a change here is a contract change.
 */
class CertificateSequenceNamesTest {

    @Test
    void derivesTheOriginalSequenceName() {
        assertEquals("certificate_seq_sc_000001", CertificateSequenceNames.original("SC-000001"));
    }

    @Test
    void derivesASeparateReplacementSequenceName() {
        // TR-004: replacement numbers come from their own series and can never reuse an original.
        assertEquals("certificate_replacement_seq_sc_000001",
                CertificateSequenceNames.replacement("SC-000001"));
        assertNotEquals(CertificateSequenceNames.original("SC-000001"),
                CertificateSequenceNames.replacement("SC-000001"));
    }

    @Test
    void namesAreIndependentPerShareClass() {
        assertNotEquals(CertificateSequenceNames.original("SC-000001"),
                CertificateSequenceNames.original("SC-000002"));
    }

    @Test
    void namesAreCaseInsensitiveOnTheId() {
        assertEquals(CertificateSequenceNames.original("SC-000001"),
                CertificateSequenceNames.original("sc-000001"));
    }

    @Test
    void producedNamesAreSafeIdentifiers() {
        assertTrue(CertificateSequenceNames.SAFE_NAME
                .matcher(CertificateSequenceNames.original("SC-000123")).matches());
    }

    @Test
    void rejectsAnIdThatWouldInjectSql() {
        // Identifiers cannot be parameterised in DDL, so the name is validated instead (SECURITY-05).
        assertThrows(IllegalArgumentException.class,
                () -> CertificateSequenceNames.original("SC-1; DROP TABLE share_class"));
        assertThrows(IllegalArgumentException.class,
                () -> CertificateSequenceNames.original("SC-1'"));
    }

    @Test
    void rejectsAMissingId() {
        assertThrows(IllegalArgumentException.class, () -> CertificateSequenceNames.original(null));
    }

    @Test
    void rejectsAnIdThatWouldOverrunTheIdentifierLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> CertificateSequenceNames.original("s".repeat(80)));
    }
}
