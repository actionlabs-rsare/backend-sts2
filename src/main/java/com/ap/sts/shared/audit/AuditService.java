package com.ap.sts.shared.audit;

import com.ap.sts.shared.auth.CurrentUser;
import com.ap.sts.shared.auth.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Append-only audit writer (AUDIT-SPRINT). Callers pass before/after snapshots that
 * must already be free of PII beyond what the audit legitimately needs (SECURITY-03/13).
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(String action, String entityRef, Object before, Object after) {
        Session session = CurrentUser.get();
        String actor = session != null ? session.userId() : "system";
        AuditEntry entry = new AuditEntry(
                actor, action, entityRef, toJson(before), toJson(after), Instant.now());
        repository.save(entry);
        log.info("audit action={} entityRef={} actor={}", action, entityRef, actor);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"_serializationError\":true}";
        }
    }
}
