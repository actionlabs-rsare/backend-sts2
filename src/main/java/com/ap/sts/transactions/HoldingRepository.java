package com.ap.sts.transactions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    Optional<Holding> findByStockholderCodeAndCompanyCodeAndShareClassId(
            String stockholderCode, String companyCode, String shareClassId);
}
