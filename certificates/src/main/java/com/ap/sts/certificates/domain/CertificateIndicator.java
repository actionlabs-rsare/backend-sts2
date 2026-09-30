package com.ap.sts.certificates.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Indicator field (TR-007; contract Certificate.indicator = '' | Replacement | Treasury). */
public enum CertificateIndicator {
    NONE(""),
    REPLACEMENT("Replacement"),
    TREASURY("Treasury");

    private final String code;

    CertificateIndicator(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    public static CertificateIndicator fromCode(String code) {
        for (CertificateIndicator i : values()) {
            if (i.code.equals(code == null ? "" : code)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown certificate indicator: " + code);
    }

    /** Stores the contract code ('' / Replacement / Treasury) rather than the Java name. */
    @Converter
    public static class DbConverter implements AttributeConverter<CertificateIndicator, String> {
        @Override
        public String convertToDatabaseColumn(CertificateIndicator attribute) {
            return (attribute == null ? NONE : attribute).code();
        }

        @Override
        public CertificateIndicator convertToEntityAttribute(String dbData) {
            return fromCode(dbData);
        }
    }
}
