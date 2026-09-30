package com.ap.sts.certificates.app;

import com.ap.sts.certificates.domain.CertificateNumbers;
import com.ap.sts.certificates.domain.CertificateOrigin;
import com.ap.sts.certificates.domain.CertificateRuleException;
import com.ap.sts.certificates.domain.NumberSequence;
import com.ap.sts.certificates.domain.NumberingMode;
import com.ap.sts.certificates.persistence.CertificateRepository;
import com.ap.sts.certificates.persistence.NumberSequenceRepository;
import com.ap.sts.certificates.persistence.SequenceGateway;
import com.ap.sts.certificates.ports.CertificatePorts.ShareClassDirectory;
import com.ap.sts.certificates.ports.CertificatePorts.ShareClassInfo;
import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * SL-002 certificate numbering per share class, with a separate replacement series (TR-004).
 * Numbers are GAPLESS and strictly incremental (S3-Q7): a database counter moves +1 inside the
 * print transaction, so a failed print gives its number back. A starting point can be set only
 * until the series issues its first number; after that it is locked. A Team-Leader-approved manual
 * number may only confirm the next number (S3-Q8), so it also comes from the counter. As a defence
 * (e.g. data migrated from AEV later, D7), a number that already exists is skipped, never reused.
 */
@Service
public class NumberingService {

    /** Guard against a runaway skip loop; far above any realistic run of manual numbers. */
    static final int MAX_SKIP = 10_000;

    private final NumberSequenceRepository sequences;
    private final CertificateRepository certificates;
    private final SequenceGateway gateway;
    private final ShareClassDirectory shareClasses;
    private final AuditService audit;

    public NumberingService(NumberSequenceRepository sequences, CertificateRepository certificates,
                            SequenceGateway gateway, ShareClassDirectory shareClasses, AuditService audit) {
        this.sequences = sequences;
        this.certificates = certificates;
        this.gateway = gateway;
        this.shareClasses = shareClasses;
        this.audit = audit;
    }

    /** Current configuration + next numbers (contract NumberingConfig). */
    public record NumberingView(String shareClassId, long originalStart, long replacementStart,
                                long nextOriginal, long nextReplacement, NumberingMode mode) {
    }

    /** Requested change; null fields are left unchanged. Starting points are typed text (e.g. "235", "R-7"). */
    public record NumberingChange(String originalStart, String replacementStart, NumberingMode mode) {
    }

    @Transactional(readOnly = true)
    public NumberingView view(String shareClassId) {
        requireShareClass(shareClassId);
        return snapshot(shareClassId);
    }

    @Transactional
    public NumberingView configure(String shareClassId, NumberingChange change) {
        ShareClassInfo shareClass = requireShareClass(shareClassId);
        if (change == null || (change.originalStart() == null && change.replacementStart() == null
                && change.mode() == null)) {
            throw new CertificateRuleException("validation_error",
                    "Nothing to change. Enter a starting number or choose a numbering mode.");
        }
        Long newOriginal = change.originalStart() == null ? null
                : CertificateNumbers.parse(CertificateOrigin.original, change.originalStart(), "originalStart");
        Long newReplacement = change.replacementStart() == null ? null
                : CertificateNumbers.parse(CertificateOrigin.replacement, change.replacementStart(), "replacementStart");

        String actor = Actors.current();
        Instant now = Instant.now();
        NumberSequence original = ensureRow(shareClassId, CertificateOrigin.original, now, actor);
        NumberSequence replacement = ensureRow(shareClassId, CertificateOrigin.replacement, now, actor);
        NumberingView before = snapshot(shareClassId);

        if (newOriginal != null) {
            setStart(shareClass, original, newOriginal, "originalStart");
        }
        if (newReplacement != null) {
            setStart(shareClass, replacement, newReplacement, "replacementStart");
        }
        NumberingMode mode = change.mode() != null ? change.mode() : original.getMode();
        original.configure(newOriginal != null ? newOriginal : original.getStartNumber(), mode, now, actor);
        replacement.configure(newReplacement != null ? newReplacement : replacement.getStartNumber(), mode, now, actor);
        sequences.save(original);
        sequences.save(replacement);

        NumberingView after = snapshot(shareClassId);
        audit.record("certificate.numbering.configure", "share-class/" + shareClassId + "/numbering", before, after);
        return after;
    }

    /**
     * Takes the next unused number of the class's series (TR-018), advancing the counter by one per
     * number. Must run inside the print transaction, which holds the counter row until it commits.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String drawNext(String shareClassId, CertificateOrigin origin) {
        ensureRow(shareClassId, origin, Instant.now(), Actors.current());
        for (int i = 0; i < MAX_SKIP; i++) {
            String number = CertificateNumbers.format(origin, gateway.next(shareClassId, origin));
            if (!certificates.existsByShareClassIdAndNumber(shareClassId, number)) {
                return number;
            }
        }
        throw new IllegalStateException("no free certificate number within " + MAX_SKIP + " draws for " + shareClassId);
    }

    /** SL-002 mode of the class's original series (Automatic until configured). */
    @Transactional(readOnly = true)
    public NumberingMode modeOf(String shareClassId) {
        return sequences.findByShareClassIdAndOrigin(shareClassId, CertificateOrigin.original)
                .map(NumberSequence::getMode).orElse(NumberingMode.Automatic);
    }

    /** The next original number the class will issue, formatted (for guidance messages). */
    @Transactional(readOnly = true)
    public String nextOriginal(String shareClassId) {
        return CertificateNumbers.format(CertificateOrigin.original, nextUnused(shareClassId, CertificateOrigin.original));
    }

    private void setStart(ShareClassInfo shareClass, NumberSequence row, long requested, String field) {
        CertificateOrigin origin = row.getOrigin();
        long next = gateway.peekNext(shareClass.id(), origin);
        boolean issued = next > row.getStartNumber();
        if (issued) {
            if (requested == row.getStartNumber()) {
                return; // unchanged
            }
            String series = origin == CertificateOrigin.replacement ? " replacement" : "";
            throw new CertificateRuleException("sequence_locked",
                    shareClass.stockType() + series + " certificates have already been issued from "
                            + CertificateNumbers.format(origin, row.getStartNumber()) + ", so the starting point is "
                            + "locked. Numbers continue from " + CertificateNumbers.format(origin, next)
                            + " without gaps (S3-Q7).", field);
        }
        gateway.setStart(shareClass.id(), origin, requested);
    }

    private NumberSequence ensureRow(String shareClassId, CertificateOrigin origin, Instant now, String actor) {
        // Serialise creation so two first prints of a new class can't both insert the row.
        gateway.lockSeries(shareClassId, origin);
        return sequences.findByShareClassIdAndOrigin(shareClassId, origin)
                .orElseGet(() -> sequences.save(new NumberSequence(shareClassId, origin, now, actor)));
    }

    private NumberingView snapshot(String shareClassId) {
        Optional<NumberSequence> original = sequences.findByShareClassIdAndOrigin(shareClassId, CertificateOrigin.original);
        Optional<NumberSequence> replacement = sequences.findByShareClassIdAndOrigin(shareClassId, CertificateOrigin.replacement);
        return new NumberingView(
                shareClassId,
                original.map(NumberSequence::getStartNumber).orElse(1L),
                replacement.map(NumberSequence::getStartNumber).orElse(1L),
                nextUnused(shareClassId, CertificateOrigin.original),
                nextUnused(shareClassId, CertificateOrigin.replacement),
                original.map(NumberSequence::getMode).orElse(NumberingMode.Automatic));
    }

    private long nextUnused(String shareClassId, CertificateOrigin origin) {
        long n = gateway.peekNext(shareClassId, origin);
        for (int i = 0; i < MAX_SKIP; i++, n++) {
            if (!certificates.existsByShareClassIdAndNumber(shareClassId, CertificateNumbers.format(origin, n))) {
                return n;
            }
        }
        return n;
    }

    private ShareClassInfo requireShareClass(String shareClassId) {
        return shareClasses.find(shareClassId).orElseThrow(() -> new NotFoundException(
                "Share class " + shareClassId + " was not found. Pick a share class from the company's list."));
    }
}
