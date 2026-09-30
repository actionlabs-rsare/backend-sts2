package com.ap.sts.approvals;

import com.ap.sts.shared.auth.Role;
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
 * A single approval step for a transaction (Approval Workflow context, SL-004). Level 1 routes
 * to a {@link Role#FirstLevelApprover}; level 2 (override/void only) routes to the
 * {@link Role#TeamLeader}. Decisions are append-only in spirit — a decided request is never
 * reopened; a rejection ends the workflow (OI-16/OI-19).
 */
@Entity
@Table(name = "transactions_approval_request")
public class ApprovalRequest {

    public enum Decision {
        Pending,
        Approved,
        Rejected
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "level", nullable = false)
    private int level;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_role", nullable = false)
    private Role requiredRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision;

    @Column(name = "actor")
    private String actor;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ApprovalRequest() {
        // JPA
    }

    public ApprovalRequest(String transactionId, int level, Role requiredRole) {
        this.transactionId = transactionId;
        this.level = level;
        this.requiredRole = requiredRole;
        this.decision = Decision.Pending;
        this.createdAt = Instant.now();
    }

    public void decide(Decision decision, String actor, String reason) {
        this.decision = decision;
        this.actor = actor;
        this.reason = reason;
        this.decidedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getLevel() {
        return level;
    }

    public Role getRequiredRole() {
        return requiredRole;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getActor() {
        return actor;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
