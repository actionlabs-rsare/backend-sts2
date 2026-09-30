package com.ap.sts.stockholders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Personal-data masking (SECURITY-03/13, security-nfr.md T7). */
class PiiTest {

    @Test
    void maskTinKeepsOnlyTheTail() {
        assertEquals("***-000", Pii.maskTin("123-456-789-000"));
    }

    @Test
    void maskTinRedactsShortValuesEntirely() {
        assertEquals("***", Pii.maskTin("1234"));
        assertEquals("***", Pii.maskTin("12"));
    }

    @Test
    void maskTinNeverRevealsTheLeadingDigits() {
        String masked = Pii.maskTin("123-456-789-000");
        assertTrue(masked.startsWith("***"));
        assertEquals(-1, masked.indexOf("123"), "leading TIN digits must not survive masking");
        assertEquals(-1, masked.indexOf("456"));
    }

    @Test
    void maskEmailKeepsOnlyTheFirstCharacterAndDomain() {
        assertEquals("m***@example.test", Pii.maskEmail("mc.santiago@example.test"));
    }

    @Test
    void maskEmailRedactsValuesWithoutADomain() {
        assertEquals("***", Pii.maskEmail("not-an-email"));
        assertEquals("***", Pii.maskEmail("@nolocalpart.test"));
    }

    @Test
    void maskMobileKeepsOnlyTheLastThreeDigits() {
        assertEquals("***001", Pii.maskMobile("+63 900 000 0001"));
        assertEquals("***", Pii.maskMobile("12"));
    }

    @Test
    void redactRevealsNothing() {
        assertEquals("***", Pii.redact("Cebu City, PH"));
    }

    @Test
    void nullsStayNullSoAnAbsentValueLogsAsAbsent() {
        assertNull(Pii.maskTin(null));
        assertNull(Pii.maskEmail(null));
        assertNull(Pii.maskMobile(null));
        assertNull(Pii.redact(null));
    }
}
