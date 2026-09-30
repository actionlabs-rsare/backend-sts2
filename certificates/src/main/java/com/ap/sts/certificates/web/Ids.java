package com.ap.sts.certificates.web;

import com.ap.sts.certificates.domain.CertificateRuleException;

/** Path/query identifier validation (SECURITY-05): reject anything that isn't a plain reference. */
final class Ids {

    private static final java.util.regex.Pattern ID = java.util.regex.Pattern.compile(CertificateDtos.ID_PATTERN);

    private Ids() {
    }

    static String requireValid(String value, String field) {
        if (value == null || !ID.matcher(value).matches()) {
            throw new CertificateRuleException("validation_error",
                    "'" + (value == null ? "" : truncate(value)) + "' is not a valid reference. Use letters, digits "
                            + "and dashes only, for example TRF-003.", field);
        }
        return value;
    }

    private static String truncate(String value) {
        return value.length() > 40 ? value.substring(0, 40) + "…" : value;
    }
}
