package com.ap.sts.stockholders;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import com.ap.sts.stockholders.StockholderDtos.FamilyGroupResponse;
import com.ap.sts.stockholders.StockholderDtos.StockholderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Family groups and the grouping inquiry (MDM-002-FG, OI-15).
 *
 * <p>Grouping is reporting only: it never changes a holding or a certificate. The combined
 * shareholdings column comes from S2's ledger through {@link HoldingsLookup}, which is a
 * fixture until Wave 2 — unknown totals stay null rather than showing as zero.
 *
 * <p>Open: the exact grouping rules (who belongs to a group, and whether the inquiry is a report
 * or an on-screen view) still need AP Corsec sign-off — OI-15 follow-up.
 */
@Service
public class FamilyGroupService {

    private final FamilyGroupRepository repository;
    private final StockholderRepository stockholders;
    private final HoldingsLookup holdings;
    private final AuditService audit;

    public FamilyGroupService(FamilyGroupRepository repository,
                              StockholderRepository stockholders,
                              HoldingsLookup holdings,
                              AuditService audit) {
        this.repository = repository;
        this.stockholders = stockholders;
        this.holdings = holdings;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<FamilyGroup> list() {
        return repository.findAllOrdered();
    }

    @Transactional
    public FamilyGroup create(String name) {
        String id = String.format("FG-%03d", repository.nextFamilyGroupIdSeq());
        FamilyGroup saved = repository.save(new FamilyGroup(id, name));
        audit.record("familyGroup.create", "familyGroup/" + id, null, FamilyGroupResponse.from(saved));
        return saved;
    }

    /**
     * Members of a group with their shareholdings. Ordered by code so the inquiry is stable
     * between refreshes.
     */
    @Transactional(readOnly = true)
    public List<StockholderResponse> members(String id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Family group not found: " + id);
        }
        return stockholders.findByFamilyGroupId(id).stream()
                .map(s -> StockholderResponse.from(s, holdings.sharesHeldBy(s.getStockholderCode())))
                .toList();
    }
}
