package com.ap.sts.stockholders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Stockholder persistence.
 *
 * <p>No delete method is exposed and none may be added: business records are never hard-deleted
 * (OI-19 / Gate 3 Q4). {@link JpaRepository} does declare deletes, so
 * {@link StockholderService} is the only entry point and offers deactivation instead.
 */
public interface StockholderRepository extends JpaRepository<Stockholder, String> {

    /**
     * All stockholders in code order. {@code findAll()} leaves ordering to the database, which makes
     * the list screen reshuffle between refreshes; the maintenance table needs a stable order.
     */
    @Query("select s from Stockholder s order by s.stockholderCode")
    List<Stockholder> findAllOrdered();

    @Query("select s from Stockholder s where lower(s.name) like lower(concat('%', :q, '%')) "
            + "or lower(s.stockholderCode) like lower(concat('%', :q, '%')) order by s.stockholderCode")
    List<Stockholder> search(String q);

    @Query("select s from Stockholder s where s.familyGroupId = :familyGroupId order by s.stockholderCode")
    List<Stockholder> findByFamilyGroupId(String familyGroupId);

    /**
     * Next value of the stockholder's own sequence (MDM-002). Deliberately a different
     * sequence from {@code company_code_seq}: stockholders are separate entities from companies.
     */
    @Query(value = "select nextval('stockholder_code_seq')", nativeQuery = true)
    long nextStockholderCodeSeq();
}
