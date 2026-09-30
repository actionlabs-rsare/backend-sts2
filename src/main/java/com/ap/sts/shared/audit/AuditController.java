package com.ap.sts.shared.audit;

import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.RequiresPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Read-only audit access (common.openapi.yaml /audit). Append-only: no write endpoints. */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditRepository repository;

    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    public record AuditView(String id, String actor, String action, String entityRef, Instant timestamp) {
    }

    public record AuditPageView(List<AuditView> items, long total) {
    }

    @GetMapping
    @RequiresPermission(module = "audit", action = Permission.list)
    public AuditPageView list(@RequestParam(required = false) String entityRef,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AuditEntry> result = (entityRef != null)
                ? repository.findByEntityRefOrderByTimestampDesc(entityRef, pageable)
                : repository.findAllByOrderByTimestampDesc(pageable);
        List<AuditView> items = result.getContent().stream()
                .map(e -> new AuditView(String.valueOf(e.getId()), e.getActor(), e.getAction(),
                        e.getEntityRef(), e.getTimestamp()))
                .toList();
        return new AuditPageView(items, result.getTotalElements());
    }
}
