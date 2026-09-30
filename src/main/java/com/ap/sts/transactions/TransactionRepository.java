package com.ap.sts.transactions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Filtered listing (all filters optional; null = ignore). Ordered newest first.
     * Used by the list endpoint and the transaction report (IR-TXN).
     */
    @Query("""
            select t from Transaction t
            where (:companyCode is null or t.companyCode = :companyCode)
              and (:status is null or t.status = :status)
              and (:from is null or t.transactionDate >= :from)
              and (:to is null or t.transactionDate <= :to)
            order by t.createdAt desc
            """)
    List<Transaction> search(String companyCode, TransactionStatus status, LocalDate from, LocalDate to);

    /** Next value of the dedicated transfer-reference sequence (TRF-000001). */
    @Query(value = "select nextval('transactions_ref_seq')", nativeQuery = true)
    long nextRefSeq();
}
