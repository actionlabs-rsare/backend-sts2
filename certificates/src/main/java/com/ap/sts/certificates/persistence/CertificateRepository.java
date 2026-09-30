package com.ap.sts.certificates.persistence;

import com.ap.sts.certificates.domain.Certificate;
import com.ap.sts.certificates.domain.CertificateStatus;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Certificate persistence. Deliberately extends the bare {@link Repository} marker instead of
 * JpaRepository, so no delete method exists (OI-19); the DB trigger backs this up.
 */
public interface CertificateRepository extends Repository<Certificate, Long> {

    Certificate save(Certificate certificate);

    Optional<Certificate> findById(Long id);

    List<Certificate> findByTransactionIdOrderByLineNo(String transactionId);

    List<Certificate> findByTransactionIdAndStatusOrderByLineNo(String transactionId, CertificateStatus status);

    boolean existsByShareClassIdAndNumber(String shareClassId, String number);
}
