package com.ap.sts.shares;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ShareClassRepository extends JpaRepository<ShareClass, String> {

    @Query("select sc from ShareClass sc where sc.companyCode = :companyCode order by sc.id")
    List<ShareClass> findByCompanyCode(String companyCode);

    @Query("select sc from ShareClass sc where sc.companyCode = :companyCode "
            + "and lower(sc.stockType) = lower(:stockType)")
    Optional<ShareClass> findByCompanyAndStockType(String companyCode, String stockType);

    @Query(value = "select nextval('share_class_id_seq')", nativeQuery = true)
    long nextShareClassIdSeq();
}
