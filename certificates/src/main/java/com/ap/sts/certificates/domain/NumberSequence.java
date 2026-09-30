package com.ap.sts.certificates.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * SL-002 numbering configuration for one (share class, origin) series. The live, gapless counter
 * ({@code next_value}) is deliberately NOT mapped: only the database functions move it
 * (S3-Q7, security-nfr T3). Never deleted (trigger + no delete path).
 */
@Entity
@Table(name = "number_sequence")
public class NumberSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_class_id", nullable = false, updatable = false, length = 64)
    private String shareClassId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private CertificateOrigin origin;

    @Column(name = "start_number", nullable = false)
    private long startNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "numbering_mode", nullable = false, length = 20)
    private NumberingMode mode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    protected NumberSequence() {
        // JPA
    }

    public NumberSequence(String shareClassId, CertificateOrigin origin, Instant now, String actor) {
        this.shareClassId = shareClassId;
        this.origin = origin;
        this.startNumber = 1; // MDM-003: each class starts at 1 (the DB counter defaults to 1 too)
        this.mode = NumberingMode.Automatic;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    /** Records the configured start + mode. The counter itself is moved by the DB function. */
    public void configure(long newStart, NumberingMode newMode, Instant now, String actor) {
        this.startNumber = newStart;
        this.mode = newMode;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    public Long getId() {
        return id;
    }

    public String getShareClassId() {
        return shareClassId;
    }

    public CertificateOrigin getOrigin() {
        return origin;
    }

    public long getStartNumber() {
        return startNumber;
    }

    public NumberingMode getMode() {
        return mode;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
