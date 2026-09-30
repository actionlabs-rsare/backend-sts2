package com.ap.sts.transactions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionReportServiceTest {

    @Mock
    private TransferService transfers;

    @InjectMocks
    private TransactionReportService service;

    private Transaction sample() {
        Transaction t = new Transaction("TRF-000001", TransactionType.Transfer, TransferType.Sale,
                "CO-001", LocalDate.of(2026, 9, 29), false);
        t.setStatus(TransactionStatus.Posted);
        t.addLine(new TransactionLine("SH-0001", "SH-0002", "SC-CO001-COMMON", 5000, "0142"));
        return t;
    }

    @Test
    void generatesFilteredPdfListingTransactions() {
        // IR-TXN (OI-14): a valid PDF that lists the filtered transactions and their current status.
        when(transfers.list(any(), any(), any(), any())).thenReturn(List.of(sample()));

        byte[] pdf = service.generate("CO-001", TransactionStatus.Posted, null, null);
        String text = new String(pdf, StandardCharsets.ISO_8859_1);

        assertTrue(text.startsWith("%PDF-1.4"), "valid PDF header");
        assertTrue(text.trim().endsWith("%%EOF"), "valid PDF trailer");
        assertTrue(text.contains("Transaction Report"));
        assertTrue(text.contains("TRF-000001"));
        assertTrue(text.contains("company=CO-001"));
    }

    @Test
    void generatesPdfWhenNoMatches() {
        when(transfers.list(any(), any(), any(), any())).thenReturn(List.of());
        byte[] pdf = service.generate(null, null, null, null);
        String text = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(text.startsWith("%PDF-1.4"));
        assertTrue(text.contains("No transactions match"));
    }
}
