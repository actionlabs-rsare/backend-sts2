package com.ap.sts.transactions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Current shareholding of a stockholder in a company's share class — the posting outcome
 * owned by S2 (unit spec). Uniquely keyed by (stockholder, company, share class). Updated
 * atomically when a transfer is posted (TR-012). Never hard-deleted (OI-19).
 */
@Entity
@Table(name = "transactions_holding",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_holding_owner",
                columnNames = {"stockholder_code", "company_code", "share_class_id"}))
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stockholder_code", nullable = false)
    private String stockholderCode;

    @Column(name = "company_code", nullable = false)
    private String companyCode;

    @Column(name = "share_class_id", nullable = false)
    private String shareClassId;

    @Column(nullable = false)
    private long shares;

    protected Holding() {
        // JPA
    }

    public Holding(String stockholderCode, String companyCode, String shareClassId, long shares) {
        this.stockholderCode = stockholderCode;
        this.companyCode = companyCode;
        this.shareClassId = shareClassId;
        this.shares = shares;
    }

    public Long getId() {
        return id;
    }

    public String getStockholderCode() {
        return stockholderCode;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getShareClassId() {
        return shareClassId;
    }

    public long getShares() {
        return shares;
    }

    public void setShares(long shares) {
        this.shares = shares;
    }
}
