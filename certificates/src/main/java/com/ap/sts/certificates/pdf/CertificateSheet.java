package com.ap.sts.certificates.pdf;

import com.ap.sts.certificates.domain.CertificateOrigin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One certificate page: the 10 fields ticked on AP's real certificate (2026-09-29) —
 * stock class, number, shares, company, holder, shares in words (derived), issue date
 * (day / month / year), President, Corporate Secretary, par value — plus the transaction reference.
 */
public record CertificateSheet(
        String number,
        CertificateOrigin origin,
        String stockType,
        long shares,
        String companyName,
        String holderName,
        LocalDate issuedOn,
        String presidentName,
        String corporateSecretaryName,
        BigDecimal parValue,
        String transactionId) {
}
