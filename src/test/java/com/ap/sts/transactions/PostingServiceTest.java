package com.ap.sts.transactions;

import com.ap.sts.shared.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostingServiceTest {

    private static final String ID = "TRF-000001";
    private static final String COMPANY = "CO-001";
    private static final String CLASS = "SC-CO001-COMMON";

    @Mock
    private TransactionRepository transactions;
    @Mock
    private HoldingRepository holdings;
    @Mock
    private TransferService transferService;
    @Mock
    private AuditService audit;

    @InjectMocks
    private PostingService service;

    private Transaction approvedTransfer() {
        Transaction t = new Transaction(ID, TransactionType.Transfer, TransferType.Sale,
                COMPANY, LocalDate.of(2026, 9, 29), false);
        t.setStatus(TransactionStatus.Approved);
        t.addLine(new TransactionLine("SH-0001", "SH-0002", CLASS, 5000, "0142"));
        when(transferService.get(ID)).thenReturn(t);
        when(transactions.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        return t;
    }

    @Test
    void markPrintedPostsAndMovesSharesAtomically() {
        // TR-012/TR-013: on print completion the holdings move and status flips to Posted together.
        Transaction t = approvedTransfer();
        Holding from = new Holding("SH-0001", COMPANY, CLASS, 10000);
        when(holdings.findByStockholderCodeAndCompanyCodeAndShareClassId("SH-0001", COMPANY, CLASS))
                .thenReturn(Optional.of(from));
        when(holdings.findByStockholderCodeAndCompanyCodeAndShareClassId("SH-0002", COMPANY, CLASS))
                .thenReturn(Optional.empty());

        Transaction posted = service.markPrinted(ID);

        assertEquals(TransactionStatus.Posted, posted.getStatus());
        assertNotNull(posted.getPostedAt());
        assertEquals(5000, from.getShares(), "transferor debited");
        // Both holdings persisted in the same posting (transferor debited + transferee created/credited).
        ArgumentCaptor<Holding> saved = ArgumentCaptor.forClass(Holding.class);
        verify(holdings, times(2)).save(saved.capture());
        Holding credited = saved.getAllValues().stream()
                .filter(h -> h.getStockholderCode().equals("SH-0002")).findFirst().orElseThrow();
        assertEquals(5000, credited.getShares(), "transferee credited");
        verify(audit).record(eq("transaction.post"), eq("transaction/" + ID), any(), any());
    }

    @Test
    void onlyApprovedCanBePosted() {
        Transaction t = new Transaction(ID, TransactionType.Transfer, TransferType.Sale,
                COMPANY, LocalDate.of(2026, 9, 29), false);
        t.setStatus(TransactionStatus.PendingFirstApproval);
        when(transferService.get(ID)).thenReturn(t);
        assertThrows(TransferValidationException.class, () -> service.markPrinted(ID));
    }

    @Test
    void postingFailsIfHoldingBecameInsufficient() {
        approvedTransfer();
        when(holdings.findByStockholderCodeAndCompanyCodeAndShareClassId("SH-0001", COMPANY, CLASS))
                .thenReturn(Optional.of(new Holding("SH-0001", COMPANY, CLASS, 3000)));
        assertThrows(TransferValidationException.class, () -> service.markPrinted(ID));
    }
}
