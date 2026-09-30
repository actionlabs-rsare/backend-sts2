package com.ap.sts.transactions;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.transactions.TransactionDtos.BreakdownEntry;
import com.ap.sts.transactions.TransactionDtos.TransferInput;
import com.ap.sts.transactions.TransactionDtos.TransferLineInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferServiceTest {

    private static final String COMPANY = "CO-001";
    private static final String CLASS = "SC-CO001-COMMON";

    @Mock
    private TransactionRepository transactions;
    @Mock
    private HoldingRepository holdings;
    @Mock
    private AuditService audit;

    @InjectMocks
    private TransferService service;

    private TransferInput oneLine(String from, String to, long shares) {
        return new TransferInput(COMPANY, LocalDate.of(2026, 9, 29), "SRF-0001",
                TransferType.Sale, "demo", false,
                List.of(new TransferLineInput(from, to, CLASS, shares, "0142")));
    }

    private void holderHas(String code, long shares) {
        when(holdings.findByStockholderCodeAndCompanyCodeAndShareClassId(code, COMPANY, CLASS))
                .thenReturn(shares == 0
                        ? Optional.empty()
                        : Optional.of(new Holding(code, COMPANY, CLASS, shares)));
    }

    @Test
    void createGeneratesPrefixedReferenceAndWritesAudit() {
        // TR-003: a transfer is created at Submitted with a TRF- reference and an audit entry.
        when(transactions.nextRefSeq()).thenReturn(1L);
        when(transactions.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        holderHas("SH-0001", 10000);
        holderHas("SH-0002", 0);

        Transaction tx = service.createTransfer(oneLine("SH-0001", "SH-0002", 5000));

        assertEquals("TRF-000001", tx.getId());
        assertEquals(TransactionStatus.Submitted, tx.getStatus());
        assertEquals(TransactionType.Transfer, tx.getType());
        verify(audit).record(eq("transaction.create"), eq("transaction/TRF-000001"), isNull(), any());
    }

    @Test
    void cannotTransferMoreThanHeld() {
        // TR-003 invariant: transferor cannot move more shares than currently held → 422.
        holderHas("SH-0001", 10000);
        TransferValidationException ex = assertThrows(TransferValidationException.class,
                () -> service.createTransfer(oneLine("SH-0001", "SH-0002", 15000)));
        assertTrue(ex.getMessage().contains("Cannot transfer more than held"));
    }

    @Test
    void transferorAndTransfereeMustDiffer() {
        holderHas("SH-0001", 10000);
        assertThrows(TransferValidationException.class,
                () -> service.createTransfer(oneLine("SH-0001", "SH-0001", 1000)));
    }

    @Test
    void breakdownConservesShares() {
        // TR-003: transferor decrease equals transferee increase; before/after reflect holdings.
        holderHas("SH-0001", 10000);
        holderHas("SH-0002", 0);
        Transaction tx = new Transaction("TRF-000009", TransactionType.Transfer, TransferType.Sale,
                COMPANY, LocalDate.of(2026, 9, 29), false);
        tx.addLine(new TransactionLine("SH-0001", "SH-0002", CLASS, 4000, "0142"));

        List<BreakdownEntry> breakdown = service.breakdown(tx);

        long totalDelta = breakdown.stream().mapToLong(BreakdownEntry::delta).sum();
        assertEquals(0, totalDelta, "shares must be conserved (net delta zero)");
        BreakdownEntry from = breakdown.stream().filter(b -> b.stockholderCode().equals("SH-0001")).findFirst().orElseThrow();
        BreakdownEntry to = breakdown.stream().filter(b -> b.stockholderCode().equals("SH-0002")).findFirst().orElseThrow();
        assertEquals(10000, from.sharesBefore());
        assertEquals(6000, from.sharesAfter());
        assertEquals(0, to.sharesBefore());
        assertEquals(4000, to.sharesAfter());
    }
}
