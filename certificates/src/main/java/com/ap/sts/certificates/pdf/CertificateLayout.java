package com.ap.sts.certificates.pdf;

/**
 * Where each of the 10 ticked fields lands on AP's certificate stock, in PDF points from the
 * bottom-left of a landscape Letter page (792 × 612). y is the text baseline.
 *
 * <p>{@link #MEASURED_FROM_SCAN}: first pass measured from the user's scan of a real certificate
 * (ABOITIZ POWER DISTRIBUTED ENERGY, INC., No. 1, 2026-09-29) at 0.64 pt/px. Scans can be skewed
 * or scaled by the scanner, so confirm with a test print on blank stock and adjust here (OI-12).
 */
public record CertificateLayout(
        Slot stockClass,
        Slot number,
        Slot shares,
        Slot company,
        Slot holder,
        Slot sharesInWords,
        Slot day,
        Slot month,
        Slot year,
        Slot president,
        Slot corporateSecretary,
        Slot parValue) {

    /** One field position: anchor x (left edge, or centre when {@code centered}), baseline y, size, width limit. */
    public record Slot(float x, float y, float size, float maxWidth, boolean centered) {
        static Slot left(float x, float y, float size, float maxWidth) {
            return new Slot(x, y, size, maxWidth, false);
        }

        static Slot center(float x, float y, float size, float maxWidth) {
            return new Slot(x, y, size, maxWidth, true);
        }
    }

    public static final CertificateLayout MEASURED_FROM_SCAN = new CertificateLayout(
            Slot.left(146, 537, 10, 200),     // 1  stock class     "COMMON"                 (top left)
            Slot.center(168, 463, 13, 130),   // 2  number          "1"                      (NUMBER box)
            Slot.center(631, 463, 13, 130),   // 3  shares          "9,995"                  (SHARES box)
            Slot.center(404, 377, 16, 590),   // 4  company         "ABOITIZ POWER …, INC."  (name box)
            Slot.center(472, 301, 11, 330),   // 5  holder          after "This Certifies that"
            Slot.center(302, 285, 9, 395),    // 6  shares in words "**NINE THOUSAND …**"
            Slot.center(338, 134, 10, 75),    // 7a day             "28TH"
            Slot.center(497, 134, 10, 120),   // 7b month           "NOVEMBER"
            Slot.center(660, 134, 10, 50),    // 7c year            "2016"
            Slot.center(302, 94, 10, 210),    // 8  President       above "President"
            Slot.center(615, 94, 10, 210),    // 9  Corp. Secretary above "Corporate Secretary"
            Slot.center(405, 67, 9, 95));     // 10 par value       "Php 1.00"              (Shares ▢ Each.)
}
