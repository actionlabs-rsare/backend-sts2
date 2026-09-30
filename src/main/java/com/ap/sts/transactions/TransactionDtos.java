package com.ap.sts.transactions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request/response DTOs matching transactions.openapi.yaml. */
public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransferLineInput(
            @NotBlank String fromStockholderCode,
            @NotBlank String toStockholderCode,
            @NotBlank String shareClassId,
            @Min(1) long shares,
            @NotBlank String sourceCertificateNumber) {
    }

    public record TransferInput(
            @NotBlank String companyCode,
            @NotNull LocalDate transactionDate,
            String srfNo,
            @NotNull TransferType transferType,
            String remarks,
            boolean exception,
            @NotEmpty @Valid List<TransferLineInput> lines) {
    }

    /** A decision/reason for an approval step or a rejection (reason mandatory on exceptions — T6). */
    public record DecisionInput(String reason) {
    }

    public record LineView(
            String fromStockholderCode,
            String toStockholderCode,
            String shareClassId,
            long shares,
            String sourceCertificateNumber) {

        static LineView from(TransactionLine l) {
            return new LineView(l.getFromStockholderCode(), l.getToStockholderCode(),
                    l.getShareClassId(), l.getShares(), l.getSourceCertificateNumber());
        }
    }

    /** Computed share breakdown of transferor/transferee (TR-003). */
    public record BreakdownEntry(
            String stockholderCode,
            String shareClassId,
            long sharesBefore,
            long delta,
            long sharesAfter) {
    }

    public record ApprovalStepView(
            int level,
            String requiredRole,
            String decision,
            String actor,
            String reason,
            Instant decidedAt) {
    }

    public record TransactionResponse(
            String id,
            String type,
            String status,
            String companyCode,
            LocalDate transactionDate,
            String transferType,
            String srfNo,
            String remarks,
            boolean exception,
            Instant createdAt,
            Instant postedAt,
            List<LineView> lines,
            List<BreakdownEntry> breakdown,
            List<ApprovalStepView> approvals) {
    }
}
