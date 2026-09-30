package com.ap.sts.transactions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Transaction report (IR-TXN, OI-14): a server-side PDF of transactions filtered by company,
 * status and/or date range. Reflects current statuses at generation time. PDF only this sprint
 * (OI-17; the BRD's CSV divergence is flagged for AP sign-off).
 */
@Service
public class TransactionReportService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'");

    private final TransferService transfers;

    public TransactionReportService(TransferService transfers) {
        this.transfers = transfers;
    }

    @Transactional(readOnly = true)
    public byte[] generate(String companyCode, TransactionStatus status, LocalDate from, LocalDate to) {
        List<Transaction> rows = transfers.list(companyCode, status, from, to);

        List<String> lines = new ArrayList<>();
        lines.add("AP Stock Transfer System - Transaction Report");
        lines.add("Generated: " + STAMP.format(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
                .atZone(ZoneOffset.UTC)));
        lines.add("Filters: " + filterSummary(companyCode, status, from, to));
        lines.add("");
        lines.add(String.format("%-14s %-10s %-16s %-20s %-8s", "Reference", "Company", "Type", "Status", "Shares"));
        lines.add("----------------------------------------------------------------------------");
        if (rows.isEmpty()) {
            lines.add("No transactions match the selected filters.");
        }
        for (Transaction t : rows) {
            long shares = t.getLines().stream().mapToLong(TransactionLine::getShares).sum();
            lines.add(String.format("%-14s %-10s %-16s %-20s %8d",
                    t.getId(), t.getCompanyCode(), t.getType().name(), t.getStatus().name(), shares));
        }
        lines.add("");
        lines.add("Total transactions: " + rows.size());
        return SimplePdf.textPage(lines);
    }

    private static String filterSummary(String companyCode, TransactionStatus status, LocalDate from, LocalDate to) {
        List<String> parts = new ArrayList<>();
        if (companyCode != null && !companyCode.isBlank()) {
            parts.add("company=" + companyCode);
        }
        if (status != null) {
            parts.add("status=" + status.name());
        }
        if (from != null) {
            parts.add("from=" + from);
        }
        if (to != null) {
            parts.add("to=" + to);
        }
        return parts.isEmpty() ? "none (all transactions)" : String.join(", ", parts);
    }
}
