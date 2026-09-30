package com.ap.sts.stockholders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stockholder aggregate root (MDM-002).
 *
 * <p>Invariants enforced here and in {@link StockholderService}:
 * <ul>
 *   <li>{@code stockholderCode} is assigned from its <b>own</b> sequence ({@code stockholder_code_seq}),
 *       never the company sequence — a stockholder is a separate entity from a company.</li>
 *   <li>TIN, nationality, gender and email are required (BRD MDM-002; gender/email are the UX-4 additions).</li>
 *   <li>Family-group membership is optional (0..1) and carries no business rule beyond the inquiry.</li>
 *   <li>Records are never deleted (OI-19 / Gate 3 Q4). {@code active} expresses the lifecycle.</li>
 * </ul>
 *
 * <p>All personal fields are personal information under the PH Data Privacy Act — see {@link Pii}.
 */
@Entity
@Table(name = "stockholder")
public class Stockholder {

    @Id
    @Column(name = "stockholder_code", nullable = false, updatable = false)
    private String stockholderCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private StockholderType type;

    @Column(name = "tin", nullable = false)
    private String tin;

    @Column(name = "nationality", nullable = false)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "corporation_type", length = 20)
    private CorporationType corporationType;

    @Column(name = "family_group_id")
    private String familyGroupId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Stockholder() {
        // JPA
    }

    public Stockholder(String stockholderCode, String name, StockholderType type) {
        this.stockholderCode = stockholderCode;
        this.name = name;
        this.type = type;
    }

    /**
     * Corporate details only apply to corporate stockholders; clearing them on an
     * individual keeps the record internally consistent (fail closed, not silently mixed).
     */
    public void normalise() {
        if (type != StockholderType.Corporate) {
            corporationType = null;
        }
    }

    public String getStockholderCode() {
        return stockholderCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StockholderType getType() {
        return type;
    }

    public void setType(StockholderType type) {
        this.type = type;
    }

    public String getTin() {
        return tin;
    }

    public void setTin(String tin) {
        this.tin = tin;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public CorporationType getCorporationType() {
        return corporationType;
    }

    public void setCorporationType(CorporationType corporationType) {
        this.corporationType = corporationType;
    }

    public String getFamilyGroupId() {
        return familyGroupId;
    }

    public void setFamilyGroupId(String familyGroupId) {
        this.familyGroupId = familyGroupId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Log-safe description. Deliberately the only stringification of this entity:
     * it contains the code and nothing that identifies the person (SECURITY-03/13).
     */
    @Override
    public String toString() {
        return "Stockholder[" + stockholderCode + "]";
    }
}
