package com.ap.sts.shares;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import com.ap.sts.shares.ShareClassDtos.ShareClassInput;
import com.ap.sts.shares.ShareClassDtos.ShareClassResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Company share maintenance (MDM-003).
 *
 * <p>Two things are enforced here rather than trusted from the client: the total amount is computed
 * from shares × par value, and each new share class gets its own certificate sequence starting at 1.
 * Share counts are validated as a nested set so a class cannot claim more issued than subscribed.
 */
@Service
public class ShareClassService {

    private static final Logger log = LoggerFactory.getLogger(ShareClassService.class);

    private final ShareClassRepository repository;
    private final CertificateSequenceStore sequences;
    private final AuditService audit;

    public ShareClassService(ShareClassRepository repository,
                             CertificateSequenceStore sequences,
                             AuditService audit) {
        this.repository = repository;
        this.sequences = sequences;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ShareClass> list(String companyCode) {
        return repository.findByCompanyCode(companyCode);
    }

    @Transactional(readOnly = true)
    public ShareClass get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Share class not found: " + id));
    }

    @Transactional
    public ShareClass create(String companyCode, ShareClassInput input) {
        repository.findByCompanyAndStockType(companyCode, input.stockType()).ifPresent(existing -> {
            // One sequence per class only works if a class is unique within its company.
            throw new ShareClassConflictException(
                    "Company " + companyCode + " already has a " + input.stockType() + " share class");
        });
        String id = String.format("SC-%06d", repository.nextShareClassIdSeq());
        ShareClass shareClass = new ShareClass(id, companyCode, input.stockType().trim(), input.parValue());
        applyCounts(shareClass, input);
        ShareClass saved = repository.save(shareClass);

        // Inside the same transaction as the insert: a share class without its numbering
        // would be unusable to S3, and a sequence without its class would be an orphan.
        sequences.createFor(id);

        audit.record("shareClass.create", ref(id), null, response(saved));
        log.info("share class created id={} company={} stockType={} total={}",
                id, companyCode, saved.getStockType(), saved.getTotalAmount());
        return saved;
    }

    @Transactional
    public ShareClass update(String id, ShareClassInput input) {
        ShareClass shareClass = get(id);
        ShareClassResponse before = response(shareClass);
        shareClass.setStockType(input.stockType().trim());
        shareClass.setParValue(input.parValue());
        applyCounts(shareClass, input);
        ShareClass saved = repository.save(shareClass);
        audit.record("shareClass.update", ref(id), before, response(saved));
        log.info("share class updated id={} total={}", id, saved.getTotalAmount());
        return saved;
    }

    /** Response view including the derived total and the next certificate number. */
    @Transactional(readOnly = true)
    public ShareClassResponse response(ShareClass shareClass) {
        Long next = sequences.peekNext(shareClass.getCertificateSequenceName());
        return ShareClassResponse.from(shareClass, next == null ? null : String.valueOf(next));
    }

    /**
     * Share counts nest: treasury ≤ issued ≤ subscribed ≤ authorized. Rejecting the combination
     * server-side keeps the certificate ceiling meaningful for S3.
     */
    private void applyCounts(ShareClass shareClass, ShareClassInput input) {
        long authorized = input.authorizedShares();
        long subscribed = orZero(input.subscribedShares());
        long issued = orZero(input.issuedShares());
        long treasury = orZero(input.treasuryShares());
        if (subscribed > authorized) {
            throw new ShareClassConflictException("Subscribed shares cannot exceed authorized shares");
        }
        if (issued > subscribed) {
            throw new ShareClassConflictException("Issued shares cannot exceed subscribed shares");
        }
        if (treasury > issued) {
            throw new ShareClassConflictException("Treasury shares cannot exceed issued shares");
        }
        shareClass.setAuthorizedShares(authorized);
        shareClass.setSubscribedShares(subscribed);
        shareClass.setIssuedShares(issued);
        shareClass.setTreasuryShares(treasury);
    }

    private static long orZero(Long value) {
        return value == null ? 0L : value;
    }

    private static String ref(String id) {
        return "shareClass/" + id;
    }
}
