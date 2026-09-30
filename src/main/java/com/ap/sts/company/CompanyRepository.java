package com.ap.sts.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, String> {

    @Query("select c from Company c where lower(c.name) like lower(concat('%', :q, '%')) "
            + "or lower(c.companyCode) like lower(concat('%', :q, '%')) order by c.companyCode")
    List<Company> search(String q);

    /** Next value of the dedicated company-code sequence (MDM-001). */
    @Query(value = "select nextval('company_code_seq')", nativeQuery = true)
    long nextCompanyCodeSeq();
}
