package com.ap.sts.certificates.pdf;

/** Share counts in English words for the certificate face, e.g. 9,995 → "Nine thousand nine hundred ninety five". */
public final class AmountInWords {

    private static final String[] ONES = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};
    private static final String[] TENS = {"", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
    private static final long[] SCALES = {1_000_000_000_000L, 1_000_000_000L, 1_000_000L, 1_000L};
    private static final String[] SCALE_NAMES = {"trillion", "billion", "million", "thousand"};

    private AmountInWords() {
    }

    public static String of(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("share counts are never negative");
        }
        if (value == 0) {
            return "Zero";
        }
        StringBuilder out = new StringBuilder();
        long rest = value;
        for (int i = 0; i < SCALES.length; i++) {
            long chunk = rest / SCALES[i];
            if (chunk > 0) {
                append(out, belowThousand((int) chunk) + " " + SCALE_NAMES[i]);
                rest %= SCALES[i];
            }
        }
        if (rest > 0) {
            append(out, belowThousand((int) rest));
        }
        return Character.toUpperCase(out.charAt(0)) + out.substring(1);
    }

    private static String belowThousand(int n) {
        StringBuilder s = new StringBuilder();
        if (n >= 100) {
            s.append(ONES[n / 100]).append(" hundred");
            n %= 100;
        }
        if (n >= 20) {
            // No hyphen, as on AP's certificates: "NINETY FIVE".
            append(s, TENS[n / 10] + (n % 10 > 0 ? " " + ONES[n % 10] : ""));
        } else if (n > 0) {
            append(s, ONES[n]);
        }
        return s.toString();
    }

    private static void append(StringBuilder s, String part) {
        if (s.length() > 0) {
            s.append(' ');
        }
        s.append(part);
    }
}
