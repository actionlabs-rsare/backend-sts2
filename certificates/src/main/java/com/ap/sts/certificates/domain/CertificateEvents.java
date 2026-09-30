package com.ap.sts.certificates.domain;

import java.time.Instant;
import java.util.List;

/**
 * Domain events published by Unit S3 (contract: events CertificateNumberAssigned, CertificatePrinted).
 * Published in-process with Spring's ApplicationEventPublisher; S2 subscribes in Wave 2.
 */
public final class CertificateEvents {

    private CertificateEvents() {
    }

    /** A number was drawn from a share-class sequence, or a Team-Leader-approved manual number was used (TR-018). */
    public record CertificateNumberAssigned(String transactionId, String shareClassId, String number,
                                            CertificateOrigin origin, boolean manual, Instant at) {
    }

    /** Certificates of a transaction were printed (TR-017). {@code reprint} = no new numbers were drawn. */
    public record CertificatePrinted(String transactionId, List<String> numbers, PrintMode mode,
                                     boolean reprint, Instant at) {
    }
}
