package com.ap.sts.stockholders;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import com.ap.sts.stockholders.StockholderDtos.StockholderInput;
import com.ap.sts.stockholders.StockholderDtos.StockholderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Stockholder maintenance (MDM-002).
 *
 * <p>Three rules shape this class:
 * <ul>
 *   <li><b>Own sequence.</b> The code comes from {@code stockholder_code_seq}, never the company
 *       sequence — stockholders are separate entities from companies.</li>
 *   <li><b>No deletion.</b> There is no delete method (OI-19 / Gate 3 Q4);
 *       {@link #deactivate(String)} flips a status flag and is audited.</li>
 *   <li><b>No personal data in logs.</b> Log lines carry the code and masked values only
 *       ({@link Pii}); the full before/after snapshot goes to the audit table, which is
 *       encrypted at rest and role-restricted (security-nfr.md §3).</li>
 * </ul>
 */
@Service
public class StockholderService {

    private static final Logger log = LoggerFactory.getLogger(StockholderService.class);

    private final StockholderRepository repository;
    private final FamilyGroupRepository familyGroups;
    private final AuditService audit;

    public StockholderService(StockholderRepository repository,
                              FamilyGroupRepository familyGroups,
                              AuditService audit) {
        this.repository = repository;
        this.familyGroups = familyGroups;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Stockholder> list(String q) {
        if (q == null || q.isBlank()) {
            // Ordered, not findAll(): an unordered list reshuffles between refreshes, which makes
            // the maintenance table hard to work with and widget tests non-deterministic.
            return repository.findAllOrdered();
        }
        return repository.search(q);
    }

    @Transactional(readOnly = true)
    public Stockholder get(String code) {
        return repository.findById(code)
                .orElseThrow(() -> new NotFoundException("Stockholder not found: " + code));
    }

    @Transactional
    public Stockholder create(StockholderInput input) {
        String code = String.format("SH-%03d", repository.nextStockholderCodeSeq());
        Stockholder stockholder = new Stockholder(code, input.name(), input.type());
        apply(stockholder, input);
        stockholder.setActive(input.active() == null || input.active());
        Stockholder saved = repository.save(stockholder);
        audit.record("stockholder.create", ref(code), null, StockholderResponse.from(saved));
        log.info("stockholder created code={} type={} tin={}",
                code, saved.getType(), Pii.maskTin(saved.getTin()));
        return saved;
    }

    @Transactional
    public Stockholder update(String code, StockholderInput input) {
        Stockholder stockholder = get(code);
        StockholderResponse before = StockholderResponse.from(stockholder);
        stockholder.setName(input.name());
        stockholder.setType(input.type());
        apply(stockholder, input);
        if (input.active() != null) {
            stockholder.setActive(input.active());
        }
        Stockholder saved = repository.save(stockholder);
        audit.record("stockholder.update", ref(code), before, StockholderResponse.from(saved));
        log.info("stockholder updated code={} active={}", code, saved.isActive());
        return saved;
    }

    /**
     * Retires a stockholder without removing the record (OI-19). This is the only "removal"
     * the system offers: the row, its history and its audit trail stay intact and queryable.
     */
    @Transactional
    public Stockholder deactivate(String code) {
        Stockholder stockholder = get(code);
        StockholderResponse before = StockholderResponse.from(stockholder);
        stockholder.setActive(false);
        Stockholder saved = repository.save(stockholder);
        audit.record("stockholder.deactivate", ref(code), before, StockholderResponse.from(saved));
        log.info("stockholder deactivated code={}", code);
        return saved;
    }

    private void apply(Stockholder stockholder, StockholderInput input) {
        stockholder.setTin(input.tin());
        stockholder.setNationality(input.nationality());
        stockholder.setGender(input.gender());
        stockholder.setEmail(input.email());
        stockholder.setMobile(blankToNull(input.mobile()));
        stockholder.setAddress(blankToNull(input.address()));
        stockholder.setCorporationType(input.corporationType());
        stockholder.setFamilyGroupId(resolveFamilyGroup(input.familyGroupId()));
        stockholder.normalise();
    }

    /**
     * Family-group membership is optional, but a non-null id must exist — a dangling reference
     * would quietly drop the stockholder out of every group inquiry (fail loud, not silently).
     */
    private String resolveFamilyGroup(String familyGroupId) {
        String id = blankToNull(familyGroupId);
        if (id == null) {
            return null;
        }
        if (!familyGroups.existsById(id)) {
            throw new NotFoundException("Family group not found: " + id);
        }
        return id;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String ref(String code) {
        return "stockholder/" + code;
    }
}
