package com.ap.sts.shares;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Share class of a company (MDM-003) — an entity under the Company aggregate.
 *
 * <p>Invariants:
 * <ul>
 *   <li>{@code totalAmount} is <b>derived</b>, never stored and never accepted from the client:
 *       authorized shares × par value. Keeping it computed means it cannot drift from its inputs.</li>
 *   <li>Share counts nest: treasury ≤ issued ≤ subscribed ≤ authorized.</li>
 *   <li>Each class owns an <b>independent</b> certificate-number sequence starting at 1, plus a
 *       separate replacement sequence (MDM-003, SL-002, TR-004). See
 *       {@link CertificateSequenceNames}.</li>
 * </ul>
 */
@Entity
@Table(name = "share_class")
public class ShareClass {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "company_code", nullable = false, updatable = false)
    private String companyCode;

    @Column(name = "stock_type", nullable = false)
    private String stockType;

    @Column(name = "par_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal parValue;

    @Column(name = "authorized_shares", nullable = false)
    private long authorizedShares;

    @Column(name = "subscribed_shares", nullable = false)
    private long subscribedShares;

    @Column(name = "issued_shares", nullable = false)
    private long issuedShares;

    @Column(name = "treasury_shares", nullable = false)
    private long treasuryShares;

    protected ShareClass() {
        // JPA
    }

    public ShareClass(String id, String companyCode, String stockType, BigDecimal parValue) {
        this.id = id;
        this.companyCode = companyCode;
        this.stockType = stockType;
        this.parValue = parValue;
    }

    /**
     * Authorized shares × par value, rounded to centavos. Derived on every read so the screen and
     * the API can never disagree with the underlying numbers (MDM-003 "automatically computes").
     */
    @Transient
    public BigDecimal getTotalAmount() {
        if (parValue == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return parValue.multiply(BigDecimal.valueOf(authorizedShares)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Name of this class's own certificate sequence — the key S3 draws numbers from. */
    @Transient
    public String getCertificateSequenceName() {
        return CertificateSequenceNames.original(id);
    }

    /** Name of this class's separate replacement sequence (TR-004). */
    @Transient
    public String getReplacementSequenceName() {
        return CertificateSequenceNames.replacement(id);
    }

    public String getId() {
        return id;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getStockType() {
        return stockType;
    }

    public void setStockType(String stockType) {
        this.stockType = stockType;
    }

    public BigDecimal getParValue() {
        return parValue;
    }

    public void setParValue(BigDecimal parValue) {
        this.parValue = parValue;
    }

    public long getAuthorizedShares() {
        return authorizedShares;
    }

    public void setAuthorizedShares(long authorizedShares) {
        this.authorizedShares = authorizedShares;
    }

    public long getSubscribedShares() {
        return subscribedShares;
    }

    public void setSubscribedShares(long subscribedShares) {
        this.subscribedShares = subscribedShares;
    }

    public long getIssuedShares() {
        return issuedShares;
    }

    public void setIssuedShares(long issuedShares) {
        this.issuedShares = issuedShares;
    }

    public long getTreasuryShares() {
        return treasuryShares;
    }

    public void setTreasuryShares(long treasuryShares) {
        this.treasuryShares = treasuryShares;
    }

    @Override
    public String toString() {
        return "ShareClass[" + id + "]";
    }
}
