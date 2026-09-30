package com.ap.sts.certificates.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * How each ticked field is written on the certificate, copied from AP's real certificate:
 * values in capitals, "9,995", "**NINE THOUSAND NINE HUNDRED NINETY FIVE**", "28TH", "NOVEMBER",
 * "2016", "Php 1.00".
 */
public final class CertificateFace {

    private CertificateFace() {
    }

    /** Names, company, stock class: printed in capitals. */
    public static String caps(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public static String sharesFigure(long shares) {
        return NumberFormat.getIntegerInstance(Locale.US).format(shares);
    }

    public static String sharesInWords(long shares) {
        return "**" + AmountInWords.of(shares).toUpperCase(Locale.ROOT) + "**";
    }

    /** Day with its English ordinal suffix in capitals: 1ST, 2ND, 3RD, 4TH, 11TH, 21ST, 28TH. */
    public static String day(LocalDate date) {
        int d = date.getDayOfMonth();
        String suffix;
        if (d >= 11 && d <= 13) {
            suffix = "TH";
        } else {
            suffix = switch (d % 10) {
                case 1 -> "ST";
                case 2 -> "ND";
                case 3 -> "RD";
                default -> "TH";
            };
        }
        return d + suffix;
    }

    public static String month(LocalDate date) {
        return date.getMonth().getDisplayName(TextStyle.FULL, Locale.US).toUpperCase(Locale.ROOT);
    }

    public static String year(LocalDate date) {
        return String.valueOf(date.getYear());
    }

    /** Par value per share, e.g. "Php 1.00". */
    public static String parValue(BigDecimal value) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "Php " + nf.format(value.setScale(2, RoundingMode.HALF_UP));
    }
}
