package com.ap.sts.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Append-only audit record (AUDIT-SPRINT / TR-006). No setters: entries are created once
 * and never mutated. DB grants also forbid UPDATE/DELETE for the app role (SECURITY-13/14).
 */
@Entity
@Table(name = "audit_entry")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_ref", nullable = false)
    private String entityRef;

    @Column(name = "before_json", columnDefinition = "text")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "text")
    private String afterJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant timestamp;

    protected AuditEntry() {
        // JPA
    }

    public AuditEntry(String actor, String action, String entityRef, String beforeJson, String afterJson, Instant timestamp) {
        this.actor = actor;
        this.action = action;
        this.entityRef = entityRef;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getEntityRef() {
        return entityRef;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
