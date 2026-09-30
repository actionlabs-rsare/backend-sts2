package com.ap.sts.certificates.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Certificate lifecycle (OI-16 status set; contracts/certificates.openapi.yaml Certificate.status).
 * Certificates are never deleted (OI-19): every end of life is a status.
 *
 * <pre>
 * Issued  -> Posted       printing completed, transaction posted (TR-013)
 * Issued  -> Voided       pre-printed form printed wrongly before posting (TR-014, flow deferred)
 * Posted  -> Transferred  source certificate of a later transfer (TR-005/TR-012)
 * Posted  -> Replaced     lost or destroyed; replacement drawn from the separate sequence (TR-004/005)
 * Posted  -> Cancelled    buy-back (TR-005, refined meaning)
 * Posted  -> Voided       posted pre-printed error, Team Leader approved (TR-014, flow deferred)
 * </pre>
 * Replaced, Cancelled, Transferred and Voided are terminal: their numbers are never reused (TR-004).
 */
public enum CertificateStatus {
    Issued, Replaced, Cancelled, Transferred, Voided, Posted;

    private static final Map<CertificateStatus, Set<CertificateStatus>> NEXT = new EnumMap<>(CertificateStatus.class);

    static {
        NEXT.put(Issued, EnumSet.of(Posted, Voided));
        NEXT.put(Posted, EnumSet.of(Transferred, Replaced, Cancelled, Voided));
        NEXT.put(Replaced, EnumSet.noneOf(CertificateStatus.class));
        NEXT.put(Cancelled, EnumSet.noneOf(CertificateStatus.class));
        NEXT.put(Transferred, EnumSet.noneOf(CertificateStatus.class));
        NEXT.put(Voided, EnumSet.noneOf(CertificateStatus.class));
    }

    public boolean canBecome(CertificateStatus next) {
        return next != null && NEXT.get(this).contains(next);
    }

    public boolean isTerminal() {
        return NEXT.get(this).isEmpty();
    }

    /** Ends of life that need a recorded reason (security-nfr T6). */
    public boolean requiresReason() {
        return this == Voided || this == Cancelled || this == Replaced;
    }
}
