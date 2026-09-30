package com.ap.sts.stockholders;

import org.springframework.stereotype.Component;

/**
 * Wave 1 stand-in for S2's holding ledger. Returns {@code null} — "unavailable" — rather than a
 * made-up number, so the family-group inquiry cannot show a figure AP might mistake for real data.
 *
 * <p>Wave 2: S2 publishes the holdings read model, S1 replaces this single class with one that
 * calls it across the frozen contract, and nothing else in the unit changes.
 */
@Component
public class FixtureHoldingsLookup implements HoldingsLookup {

    @Override
    public Long sharesHeldBy(String stockholderCode) {
        return null;
    }
}
