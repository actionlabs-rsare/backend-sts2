package com.ap.sts.certificates.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Certificate aggregate (Certificate &amp; Numbering context). The number, share class, origin,
 * transaction and the printed face — the 10 fields ticked on AP's real certificate — are fixed at
 * issuance (no setters; a DB trigger also rejects changes). The only mutation is a status
 * transition, and there is no delete (OI-19). Cross-context references are IDs; the face values are
 * a snapshot taken at print time so a reprint shows exactly what was signed.
 */
@Entity
@Table(name = "certificate")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_number", nullable = false, updatable = false, length = 20)
    private String number;

    @Column(name = "share_class_id", nullable = false, updatable = false, length = 64)
    private String shareClassId;

    @Column(name = "company_code", nullable = false, updatable = false, length = 20)
    private String companyCode;

    @Column(name = "holder_stockholder_code", nullable = false, updatable = false, length = 40)
    private String holderStockholderCode;

    @Column(nullable = false, updatable = false)
    private long shares;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificateStatus status;

    @Convert(converter = CertificateIndicator.DbConverter.class)
    @Column(nullable = false, length = 20)
    private CertificateIndicator indicator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private CertificateOrigin origin;

    @Column(name = "transaction_id", nullable = false, updatable = false, length = 40)
    private String transactionId;

    @Column(name = "line_no", nullable = false, updatable = false)
    private int lineNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "print_mode", nullable = false, updatable = false, length = 20)
    private PrintMode printMode;

    @Column(name = "manually_assigned", nullable = false, updatable = false)
    private boolean manuallyAssigned;

    @Column(name = "approved_by", updatable = false, length = 100)
    private String approvedBy;

    // ---- printed face (snapshot) ----
    @Column(name = "issued_on", nullable = false, updatable = false)
    private LocalDate issuedOn;

    @Column(name = "stock_type", nullable = false, updatable = false, length = 100)
    private String stockType;

    @Column(name = "company_name", nullable = false, updatable = false, length = 200)
    private String companyName;

    /** Always a company name (S3-Q9: no individual holders on certificates). Still never logged or audited. */
    @Column(name = "holder_name", nullable = false, updatable = false, length = 200)
    private String holderName;

    @Column(name = "par_value", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal parValue;

    @Column(name = "president_name", nullable = false, updatable = false, length = 200)
    private String presidentName;

    @Column(name = "corporate_secretary_name", nullable = false, updatable = false, length = 200)
    private String corporateSecretaryName;

    @Column(name = "printed_at", nullable = false, updatable = false)
    private Instant printedAt;

    @Column(name = "printed_by", nullable = false, updatable = false, length = 100)
    private String printedBy;

    @Version
    @Column(nullable = false)
    private int version;

    protected Certificate() {
        // JPA
    }

    private Certificate(Builder b) {
        this.number = b.number;
        this.shareClassId = b.shareClassId;
        this.companyCode = b.companyCode;
        this.holderStockholderCode = b.holderStockholderCode;
        this.shares = b.shares;
        this.status = CertificateStatus.Issued; // TR-004: a newly generated certificate is 'Issued'
        this.indicator = b.origin == CertificateOrigin.replacement
                ? CertificateIndicator.REPLACEMENT : CertificateIndicator.NONE;
        this.origin = b.origin;
        this.transactionId = b.transactionId;
        this.lineNo = b.lineNo;
        this.printMode = b.printMode;
        this.manuallyAssigned = b.approvedBy != null;
        this.approvedBy = b.approvedBy;
        this.issuedOn = b.issuedOn;
        this.stockType = b.stockType;
        this.companyName = b.companyName;
        this.holderName = b.holderName;
        this.parValue = b.parValue;
        this.presidentName = b.presidentName;
        this.corporateSecretaryName = b.corporateSecretaryName;
        this.printedAt = b.printedAt;
        this.printedBy = b.printedBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Moves the certificate to its next lifecycle status; illegal moves are rejected (never a delete). */
    public void transitionTo(CertificateStatus next) {
        if (!status.canBecome(next)) {
            throw new CertificateRuleException("invalid_status_change",
                    "Certificate " + number + " is " + status + " and cannot become " + next + "."
                            + (status.isTerminal() ? " " + status + " is final; its number is never reused." : ""),
                    "status");
        }
        this.status = next;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getShareClassId() {
        return shareClassId;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getHolderStockholderCode() {
        return holderStockholderCode;
    }

    public long getShares() {
        return shares;
    }

    public CertificateStatus getStatus() {
        return status;
    }

    public CertificateIndicator getIndicator() {
        return indicator;
    }

    public CertificateOrigin getOrigin() {
        return origin;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getLineNo() {
        return lineNo;
    }

    public PrintMode getPrintMode() {
        return printMode;
    }

    public boolean isManuallyAssigned() {
        return manuallyAssigned;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public String getStockType() {
        return stockType;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getHolderName() {
        return holderName;
    }

    public BigDecimal getParValue() {
        return parValue;
    }

    public String getPresidentName() {
        return presidentName;
    }

    public String getCorporateSecretaryName() {
        return corporateSecretaryName;
    }

    public Instant getPrintedAt() {
        return printedAt;
    }

    public String getPrintedBy() {
        return printedBy;
    }

    /** Assembles a new certificate at print time. */
    public static final class Builder {
        private String number;
        private String shareClassId;
        private String companyCode;
        private String holderStockholderCode;
        private long shares;
        private CertificateOrigin origin = CertificateOrigin.original;
        private String transactionId;
        private int lineNo;
        private PrintMode printMode;
        private String approvedBy;
        private LocalDate issuedOn;
        private String stockType;
        private String companyName;
        private String holderName;
        private BigDecimal parValue;
        private String presidentName;
        private String corporateSecretaryName;
        private Instant printedAt;
        private String printedBy;

        private Builder() {
        }

        public Builder number(String v) {
            this.number = v;
            return this;
        }

        public Builder shareClassId(String v) {
            this.shareClassId = v;
            return this;
        }

        public Builder companyCode(String v) {
            this.companyCode = v;
            return this;
        }

        public Builder holderStockholderCode(String v) {
            this.holderStockholderCode = v;
            return this;
        }

        public Builder shares(long v) {
            this.shares = v;
            return this;
        }

        public Builder origin(CertificateOrigin v) {
            this.origin = v;
            return this;
        }

        public Builder transactionId(String v) {
            this.transactionId = v;
            return this;
        }

        public Builder lineNo(int v) {
            this.lineNo = v;
            return this;
        }

        public Builder printMode(PrintMode v) {
            this.printMode = v;
            return this;
        }

        /** Set only for a Team-Leader-approved manual number (TR-018). */
        public Builder manuallyApprovedBy(String v) {
            this.approvedBy = v;
            return this;
        }

        public Builder issuedOn(LocalDate v) {
            this.issuedOn = v;
            return this;
        }

        public Builder stockType(String v) {
            this.stockType = v;
            return this;
        }

        public Builder companyName(String v) {
            this.companyName = v;
            return this;
        }

        public Builder holderName(String v) {
            this.holderName = v;
            return this;
        }

        public Builder parValue(BigDecimal v) {
            this.parValue = v;
            return this;
        }

        public Builder presidentName(String v) {
            this.presidentName = v;
            return this;
        }

        public Builder corporateSecretaryName(String v) {
            this.corporateSecretaryName = v;
            return this;
        }

        public Builder printedAt(Instant v) {
            this.printedAt = v;
            return this;
        }

        public Builder printedBy(String v) {
            this.printedBy = v;
            return this;
        }

        public Certificate build() {
            if (number == null || shareClassId == null || companyCode == null || holderStockholderCode == null
                    || transactionId == null || printMode == null || printedAt == null || printedBy == null
                    || origin == null) {
                throw new IllegalStateException("certificate is missing a required field");
            }
            if (issuedOn == null || isBlank(stockType) || isBlank(companyName) || isBlank(holderName)
                    || parValue == null || isBlank(presidentName) || isBlank(corporateSecretaryName)) {
                throw new IllegalStateException("certificate is missing a printed field");
            }
            if (shares < 1) {
                throw new IllegalStateException("a certificate must carry at least one share");
            }
            if (lineNo < 1) {
                throw new IllegalStateException("line numbers start at 1");
            }
            return new Certificate(this);
        }

        private static boolean isBlank(String s) {
            return s == null || s.isBlank();
        }
    }
}
