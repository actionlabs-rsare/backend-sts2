package com.ap.sts.shared.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntry, Long> {
    Page<AuditEntry> findByEntityRefOrderByTimestampDesc(String entityRef, Pageable pageable);

    Page<AuditEntry> findAllByOrderByTimestampDesc(Pageable pageable);
}
