package com.ap.sts.certificates.persistence;

import com.ap.sts.certificates.domain.CertificateOrigin;
import com.ap.sts.certificates.domain.NumberSequence;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/** SL-002 configuration rows. No delete method by design (OI-19). */
public interface NumberSequenceRepository extends Repository<NumberSequence, Long> {

    NumberSequence save(NumberSequence sequence);

    Optional<NumberSequence> findByShareClassIdAndOrigin(String shareClassId, CertificateOrigin origin);

    List<NumberSequence> findByShareClassId(String shareClassId);
}
