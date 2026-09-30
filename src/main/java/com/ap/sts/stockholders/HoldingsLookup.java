package com.ap.sts.stockholders;

/**
 * Read-only view of how many shares a stockholder holds.
 *
 * <p>The holding ledger belongs to Unit S2 (Transactions &amp; Posting). Until Wave 2 replaces
 * this with a real call across the frozen {@code transactions} contract, S1 codes against this
 * seam with {@link FixtureHoldingsLookup} — the family-group inquiry (MDM-002-FG) needs
 * combined shareholdings but must not reach into another unit's tables.
 */
public interface HoldingsLookup {

    /**
     * Shares held by a stockholder, or {@code null} when the figure is genuinely unknown.
     * Callers must render null as "unavailable", never as zero.
     */
    Long sharesHeldBy(String stockholderCode);
}
