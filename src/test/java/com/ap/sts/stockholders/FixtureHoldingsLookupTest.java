package com.ap.sts.stockholders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The Wave 1 stand-in for S2's holding ledger. It must report "unknown", not "zero" — a zero would
 * appear on the family-group inquiry as a real shareholding figure.
 */
class FixtureHoldingsLookupTest {

    private final HoldingsLookup lookup = new FixtureHoldingsLookup();

    @Test
    void reportsUnknownRatherThanZero() {
        assertNull(lookup.sharesHeldBy("SH-001"));
        assertNull(lookup.sharesHeldBy("SH-999"));
    }
}
