package com.ap.sts.transactions;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import com.ap.sts.transactions.TransactionDtos.ApprovalStepView;
import com.ap.sts.transactions.TransactionDtos.BreakdownEntry;
import com.ap.sts.transactions.TransactionDtos.LineView;
import com.ap.sts.transactions.TransactionDtos.TransactionResponse;
import com.ap.sts.transactions.TransactionDtos.TransferInput;
import com.ap.sts.transactions.TransactionDtos.TransferLineInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Transfer processing (TR-003): creates a transfer, enforces the share-conservation invariant
 * (Σ transferor decrease = Σ transferee increase; cannot transfer more than held) and computes
 * the transferor/transferee breakdown. Only transfers are processed here — additional
 * subscriptions are not (TR-003). Posting is handled by {@link PostingService}.
 */
@Service
public class TransferService {

    private final TransactionRepository transactions;
    private final HoldingRepository holdings;
    private final AuditService audit;

    public TransferService(TransactionRepository transactions, HoldingRepository holdings, AuditService audit) {
        this.transactions = transactions;
        this.holdings = holdings;
        this.audit = audit;
    }

    @Transactional
    public Transaction createTransfer(TransferInput input) {
        validate(input);

        String id = String.format("TRF-%06d", transactions.nextRefSeq());
        Transaction tx = new Transaction(id, TransactionType.Transfer, input.transferType(),
                input.companyCode(), input.transactionDate(), input.exception());
        tx.setSrfNo(input.srfNo());
        tx.setRemarks(input.remarks());
        for (TransferLineInput l : input.lines()) {
            tx.addLine(new TransactionLine(l.fromStockholderCode(), l.toStockholderCode(),
                    l.shareClassId(), l.shares(), l.sourceCertificateNumber()));
        }
        Transaction saved = transactions.save(tx);
        audit.record("transaction.create", entityRef(id), null, toResponse(saved, List.of()));
        return saved;
    }

    /** Enforces TR-003 invariants. Throws {@link TransferValidationException} (→ 422) on breach. */
    private void validate(TransferInput input) {
        // Sum outgoing shares per (transferor, share class); a transferor cannot move more than held.
        Map<String, Long> outgoing = new LinkedHashMap<>();
        for (TransferLineInput l : input.lines()) {
            if (l.fromStockholderCode().equals(l.toStockholderCode())) {
                throw new TransferValidationException(
                        "Transferor and transferee must differ (line for " + l.fromStockholderCode() + ").");
            }
            outgoing.merge(l.fromStockholderCode() + "\u0000" + l.shareClassId(), l.shares(), Long::sum);
        }
        for (Map.Entry<String, Long> e : outgoing.entrySet()) {
            String[] parts = e.getKey().split("\u0000", 2);
            long held = heldShares(parts[0], input.companyCode(), parts[1]);
            if (e.getValue() > held) {
                throw new TransferValidationException(String.format(
                        "Cannot transfer more than held: %s holds %d share(s) of class %s but the transfer moves %d.",
                        parts[0], held, parts[1], e.getValue()));
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Transaction> list(String companyCode, TransactionStatus status, LocalDate from, LocalDate to) {
        return transactions.search(blankToNull(companyCode), status, from, to);
    }

    @Transactional(readOnly = true)
    public Transaction get(String id) {
        return transactions.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }

    /**
     * Builds the API response including the computed share breakdown (TR-003). {@code approvals}
     * is supplied by the caller (the Approval Workflow owns those steps).
     */
    public TransactionResponse toResponse(Transaction tx, List<ApprovalStepView> approvals) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType().name(),
                tx.getStatus().name(),
                tx.getCompanyCode(),
                tx.getTransactionDate(),
                tx.getTransferType().name(),
                tx.getSrfNo(),
                tx.getRemarks(),
                tx.isException(),
                tx.getCreatedAt(),
                tx.getPostedAt(),
                tx.getLines().stream().map(LineView::from).toList(),
                breakdown(tx),
                approvals);
    }

    /** Net share movement per (stockholder, share class), with before/after from current holdings. */
    List<BreakdownEntry> breakdown(Transaction tx) {
        // TreeMap keeps the breakdown deterministic (stockholder, then share class).
        Map<String, long[]> deltas = new TreeMap<>();
        for (TransactionLine l : tx.getLines()) {
            deltas.computeIfAbsent(l.getFromStockholderCode() + "\u0000" + l.getShareClassId(), k -> new long[1])[0]
                    -= l.getShares();
            deltas.computeIfAbsent(l.getToStockholderCode() + "\u0000" + l.getShareClassId(), k -> new long[1])[0]
                    += l.getShares();
        }
        List<BreakdownEntry> out = new ArrayList<>();
        for (Map.Entry<String, long[]> e : deltas.entrySet()) {
            String[] parts = e.getKey().split("\u0000", 2);
            long delta = e.getValue()[0];
            long before = heldShares(parts[0], tx.getCompanyCode(), parts[1]);
            out.add(new BreakdownEntry(parts[0], parts[1], before, delta, before + delta));
        }
        return out;
    }

    private long heldShares(String stockholderCode, String companyCode, String shareClassId) {
        return holdings.findByStockholderCodeAndCompanyCodeAndShareClassId(stockholderCode, companyCode, shareClassId)
                .map(Holding::getShares)
                .orElse(0L);
    }

    private static String entityRef(String id) {
        return "transaction/" + id;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
