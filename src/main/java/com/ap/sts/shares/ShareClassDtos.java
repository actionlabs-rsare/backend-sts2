package com.ap.sts.shares;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request/response DTOs for {@code contracts/shares.openapi.yaml}.
 *
 * <p>{@code totalAmount} and {@code nextCertificateNumber} are response-only: both are derived
 * server-side, so accepting them from a client would be a way to falsify them.
 */
public final class ShareClassDtos {

    private ShareClassDtos() {
    }

    public record ShareClassInput(
            @NotBlank @Size(max = 60) String stockType,
            @NotNull @DecimalMin("0.0") BigDecimal parValue,
            @NotNull @Min(0) Long authorizedShares,
            @Min(0) Long subscribedShares,
            @Min(0) Long issuedShares,
            @Min(0) Long treasuryShares) {
    }

    public record ShareClassResponse(
            String id,
            String companyCode,
            String stockType,
            BigDecimal parValue,
            long authorizedShares,
            long subscribedShares,
            long issuedShares,
            long treasuryShares,
            BigDecimal totalAmount,
            String nextCertificateNumber) {

        public static ShareClassResponse from(ShareClass sc, String nextCertificateNumber) {
            return new ShareClassResponse(
                    sc.getId(), sc.getCompanyCode(), sc.getStockType(), sc.getParValue(),
                    sc.getAuthorizedShares(), sc.getSubscribedShares(), sc.getIssuedShares(),
                    sc.getTreasuryShares(), sc.getTotalAmount(), nextCertificateNumber);
        }
    }
}
