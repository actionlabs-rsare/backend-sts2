package com.ap.sts.certificates.ports.fixtures;

import com.ap.sts.certificates.ports.CertificatePorts.PrintableLine;
import com.ap.sts.certificates.ports.CertificatePorts.PrintableTransaction;
import com.ap.sts.certificates.ports.CertificatePorts.ShareClassDirectory;
import com.ap.sts.certificates.ports.CertificatePorts.ShareClassInfo;
import com.ap.sts.certificates.ports.CertificatePorts.StockholderDirectory;
import com.ap.sts.certificates.ports.CertificatePorts.StockholderRef;
import com.ap.sts.certificates.ports.CertificatePorts.TransactionSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WAVE 1 FIXTURES — SYNTHETIC DATA ONLY (no AEV data, D7). Stand-ins for Unit S1 (share classes,
 * stockholders) and Unit S2 (approved transfers) until the Wave 2 integration replaces these beans
 * with adapters over the real units. IDs and names are invented and marked (SAMPLE).
 * Company codes match the Wave 0 seed (CO-001..CO-003) so the TR-017 officers come from real rows.
 */
public final class Wave1Fixtures {

    private Wave1Fixtures() {
    }

    public static final Map<String, ShareClassInfo> SHARE_CLASSES = Map.of(
            "SC-CO-001-COM", new ShareClassInfo("SC-CO-001-COM", "CO-001", "Common", new BigDecimal("100.00")),
            "SC-CO-001-RPF", new ShareClassInfo("SC-CO-001-RPF", "CO-001", "Redeemable Preferred", new BigDecimal("10.00")),
            "SC-CO-002-COM", new ShareClassInfo("SC-CO-002-COM", "CO-002", "Common", new BigDecimal("1.00")),
            "SC-CO-003-COM", new ShareClassInfo("SC-CO-003-COM", "CO-003", "Common", new BigDecimal("1.00")));

    /** Holders are companies (S3-Q9); SH-0005 is the one individual, to show that printing refuses it. */
    public static final Map<String, StockholderRef> STOCKHOLDERS = Map.of(
            "SH-0001", new StockholderRef("SH-0001", "Sample Renewables, Inc. (SAMPLE)", "Corporate"),
            "SH-0002", new StockholderRef("SH-0002", "Sample Power Ventures, Inc. (SAMPLE)", "Corporate"),
            "SH-0003", new StockholderRef("SH-0003", "Sample Energy Holdings Corp. (SAMPLE)", "Corporate"),
            "SH-0004", new StockholderRef("SH-0004", "Sample Holdings Corp. (SAMPLE)", "Corporate"),
            "SH-0005", new StockholderRef("SH-0005", "Villanueva, Ana (SAMPLE)", "Individual"));

    /**
     * Approved transfers ready to print, plus one not yet approved (TRF-904) and one to an individual
     * holder (TRF-906), which printing refuses (S3-Q9).
     */
    public static final Map<String, PrintableTransaction> TRANSACTIONS = Map.of(
            // Sale of 3,000 of 5,000 shares: new certificate to the transferee + balance back to the transferor.
            "TRF-901", new PrintableTransaction("TRF-901", "Approved", "CO-001", List.of(
                    new PrintableLine("SH-0001", "SH-0002", "SC-CO-001-COM", 3_000, "142"),
                    new PrintableLine("SH-0001", "SH-0001", "SC-CO-001-COM", 2_000, "142"))),
            // Single certificate: used to demo a Team-Leader-approved manual number (TR-018).
            "TRF-902", new PrintableTransaction("TRF-902", "Approved", "CO-001", List.of(
                    new PrintableLine("SH-0003", "SH-0004", "SC-CO-001-RPF", 500, "77"))),
            "TRF-903", new PrintableTransaction("TRF-903", "Approved", "CO-002", List.of(
                    new PrintableLine("SH-0002", "SH-0003", "SC-CO-002-COM", 1_000, "310"))),
            "TRF-904", new PrintableTransaction("TRF-904", "PendingFirstApproval", "CO-001", List.of(
                    new PrintableLine("SH-0002", "SH-0005", "SC-CO-001-COM", 250, "150"))),
            "TRF-905", new PrintableTransaction("TRF-905", "Approved", "CO-003", List.of(
                    new PrintableLine("SH-0004", "SH-0003", "SC-CO-003-COM", 12_500, "21"),
                    new PrintableLine("SH-0004", "SH-0001", "SC-CO-003-COM", 7_500, "21"))),
            "TRF-906", new PrintableTransaction("TRF-906", "Approved", "CO-001", List.of(
                    new PrintableLine("SH-0002", "SH-0005", "SC-CO-001-COM", 100, "150"))));

    @Component
    public static class FixtureTransactionSource implements TransactionSource {
        @Override
        public Optional<PrintableTransaction> find(String transactionId) {
            return Optional.ofNullable(TRANSACTIONS.get(transactionId));
        }
    }

    @Component
    public static class FixtureShareClassDirectory implements ShareClassDirectory {
        @Override
        public Optional<ShareClassInfo> find(String shareClassId) {
            return Optional.ofNullable(SHARE_CLASSES.get(shareClassId));
        }
    }

    @Component
    public static class FixtureStockholderDirectory implements StockholderDirectory {
        @Override
        public Optional<StockholderRef> find(String stockholderCode) {
            return Optional.ofNullable(STOCKHOLDERS.get(stockholderCode));
        }
    }
}
