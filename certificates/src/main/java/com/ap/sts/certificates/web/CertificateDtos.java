package com.ap.sts.certificates.web;

import com.ap.sts.certificates.app.NumberingService.NumberingView;
import com.ap.sts.certificates.domain.Certificate;
import com.ap.sts.certificates.domain.CertificateNumbers;
import com.ap.sts.certificates.domain.CertificateOrigin;
import com.ap.sts.certificates.domain.CertificateStatus;
import com.ap.sts.certificates.domain.NumberingMode;
import com.ap.sts.certificates.domain.PrintMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request/response bodies matching contracts/certificates.openapi.yaml (field names are checked by CertificatesContractTest). */
public final class CertificateDtos {

    /** Transaction references: letters, digits and dashes (e.g. TRF-003). Also keeps file names/header values safe. */
    public static final String ID_PATTERN = "^[A-Za-z0-9][A-Za-z0-9-]{0,39}$";

    private CertificateDtos() {
    }

    /** #/components/schemas/NumberingConfig. Read-only fields (next*) are ignored on PUT. */
    public record NumberingConfig(
            String shareClassId,
            @Size(max = 20) String originalStart,
            @Size(max = 20) String replacementStart,
            String nextOriginal,
            String nextReplacement,
            NumberingMode mode) {

        static NumberingConfig from(NumberingView v) {
            return new NumberingConfig(
                    v.shareClassId(),
                    CertificateNumbers.format(CertificateOrigin.original, v.originalStart()),
                    CertificateNumbers.format(CertificateOrigin.replacement, v.replacementStart()),
                    CertificateNumbers.format(CertificateOrigin.original, v.nextOriginal()),
                    CertificateNumbers.format(CertificateOrigin.replacement, v.nextReplacement()),
                    v.mode());
        }
    }

    /** POST /certificates/print request body. */
    public record PrintRequest(
            @NotBlank @Pattern(regexp = ID_PATTERN, message = "use the transaction reference, e.g. TRF-003")
            String transactionId,
            @NotNull(message = "choose PrePrintedForm or SystemTemplate") PrintMode mode,
            @Size(max = 20) String manualNumber,
            Boolean teamLeaderApproval) {
    }

    /** #/components/schemas/Certificate. */
    public record CertificateResponse(
            String number,
            String shareClassId,
            String holderStockholderCode,
            long shares,
            CertificateStatus status,
            String indicator,
            CertificateOrigin origin) {

        static CertificateResponse from(Certificate c) {
            return new CertificateResponse(c.getNumber(), c.getShareClassId(), c.getHolderStockholderCode(),
                    c.getShares(), c.getStatus(), c.getIndicator().code(), c.getOrigin());
        }
    }
}
