package com.ap.sts.transactions;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Transfer transaction aggregate root (TR-003). The {@code id} is a human-readable reference
 * ({@code TRF-000001}) assigned from a dedicated sequence — 3-letter prefix + sequence
 * (applied deviation, Gate 2). Holds its transfer lines; posting produces {@code holding} rows.
 *
 * <p>{@code exception = true} marks an override/void transaction, which routes through a second
 * approval by the Team Leader (SL-004, OI-16). Standard transfers need only the first approval.
 */
@Entity
@Table(name = "transactions_transaction")
public class Transaction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false)
    private TransferType transferType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "company_code", nullable = false)
    private String companyCode;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "srf_no")
    private String srfNo;

    @Column(name = "remarks")
    private String remarks;

    /** Override/void transaction → requires a second (Team Leader) approval (SL-004, OI-16). */
    @Column(name = "is_exception", nullable = false)
    private boolean exception;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @jakarta.persistence.JoinColumn(name = "transaction_id", nullable = false)
    @OrderColumn(name = "line_no")
    private List<TransactionLine> lines = new ArrayList<>();

    protected Transaction() {
        // JPA
    }

    public Transaction(String id, TransactionType type, TransferType transferType, String companyCode,
                       LocalDate transactionDate, boolean exception) {
        this.id = id;
        this.type = type;
        this.transferType = transferType;
        this.companyCode = companyCode;
        this.transactionDate = transactionDate;
        this.exception = exception;
        this.status = TransactionStatus.Submitted;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getSrfNo() {
        return srfNo;
    }

    public void setSrfNo(String srfNo) {
        this.srfNo = srfNo;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public boolean isException() {
        return exception;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public List<TransactionLine> getLines() {
        return lines;
    }

    public void addLine(TransactionLine line) {
        this.lines.add(line);
    }
}
