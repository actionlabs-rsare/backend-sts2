package com.ap.sts.transactions;

import com.ap.sts.shared.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Posting (TR-012/TR-013). Posting is triggered by print completion ({@link #markPrinted})
 * and updates the transaction status and the affected holdings <b>atomically</b> in one DB
 * transaction. Only an {@link TransactionStatus#Approved} transaction can be posted.
 */
@Service
public class PostingService {

    private final TransactionRepository transactions;
    private final HoldingRepository holdings;
    private final TransferService transferService;
    private final AuditService audit;

    public PostingService(TransactionRepository transactions, HoldingRepository holdings,
                          TransferService transferService, AuditService audit) {
        this.transactions = transactions;
        this.holdings = holdings;
        this.transferService = transferService;
        this.audit = audit;
    }

    /**
     * Print-completion trigger (TR-013): posts an approved transfer. Decrements each transferor's
     * holding and credits each transferee, then flips the status to {@link TransactionStatus#Posted}.
     * All changes commit together (TR-012).
     */
    @Transactional
    public Transaction markPrinted(String id) {
        Transaction tx = transferService.get(id);
        if (tx.getStatus() != TransactionStatus.Approved) {
            throw new TransferValidationException(
                    "Only an Approved transaction can be posted on print completion (current: " + tx.getStatus() + ").");
        }

        for (TransactionLine line : tx.getLines()) {
            Holding from = holdings.findByStockholderCodeAndCompanyCodeAndShareClassId(
                    line.getFromStockholderCode(), tx.getCompanyCode(), line.getShareClassId())
                    .orElseThrow(() -> new TransferValidationException(
                            "Transferor holding no longer exists; cannot post."));
            if (from.getShares() < line.getShares()) {
                throw new TransferValidationException(
                        "Transferor holding changed and is now insufficient; cannot post.");
            }
            from.setShares(from.getShares() - line.getShares());
            holdings.save(from);

            Holding to = holdings.findByStockholderCodeAndCompanyCodeAndShareClassId(
                    line.getToStockholderCode(), tx.getCompanyCode(), line.getShareClassId())
                    .orElseGet(() -> new Holding(line.getToStockholderCode(), tx.getCompanyCode(),
                            line.getShareClassId(), 0L));
            to.setShares(to.getShares() + line.getShares());
            holdings.save(to);
        }

        TransactionStatus before = tx.getStatus();
        tx.setStatus(TransactionStatus.Posted);
        tx.setPostedAt(Instant.now());
        Transaction saved = transactions.save(tx);

        audit.record("transaction.post", "transaction/" + id, statusSnapshot(before), statusSnapshot(saved.getStatus()));
        return saved;
    }

    private static Map<String, Object> statusSnapshot(TransactionStatus status) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status.name());
        return m;
    }
}
