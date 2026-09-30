package com.ap.sts.transactions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One transferor → transferee move of shares within a transfer (TR-003). Cross-context
 * references (stockholder codes, share class id, certificate number) are held <b>by value/ID
 * only</b> — S2 never imports S1/S3 code (unit spec).
 */
@Entity
@Table(name = "transactions_transaction_line")
public class TransactionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_stockholder_code", nullable = false)
    private String fromStockholderCode;

    @Column(name = "to_stockholder_code", nullable = false)
    private String toStockholderCode;

    @Column(name = "share_class_id", nullable = false)
    private String shareClassId;

    @Column(nullable = false)
    private long shares;

    @Column(name = "source_certificate_number", nullable = false)
    private String sourceCertificateNumber;

    protected TransactionLine() {
        // JPA
    }

    public TransactionLine(String fromStockholderCode, String toStockholderCode, String shareClassId,
                           long shares, String sourceCertificateNumber) {
        this.fromStockholderCode = fromStockholderCode;
        this.toStockholderCode = toStockholderCode;
        this.shareClassId = shareClassId;
        this.shares = shares;
        this.sourceCertificateNumber = sourceCertificateNumber;
    }

    public Long getId() {
        return id;
    }

    public String getFromStockholderCode() {
        return fromStockholderCode;
    }

    public String getToStockholderCode() {
        return toStockholderCode;
    }

    public String getShareClassId() {
        return shareClassId;
    }

    public long getShares() {
        return shares;
    }

    public String getSourceCertificateNumber() {
        return sourceCertificateNumber;
    }
}
