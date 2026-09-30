package com.ap.sts.certificates.ports;

import com.ap.sts.certificates.ports.CertificatePorts.CompanyDirectory;
import com.ap.sts.certificates.ports.CertificatePorts.CompanyProfile;
import com.ap.sts.company.CompanyRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads company name + President + Corporate Secretary from the Wave 0 Company module (MDM-001,
 * integrator-owned, read-only use). TR-017: system templates auto-populate these officers.
 * This adapter is the only S3 class that touches another module (CR-S3-02).
 */
@Component
public class CompanyRecordDirectory implements CompanyDirectory {

    private final CompanyRepository companies;

    public CompanyRecordDirectory(CompanyRepository companies) {
        this.companies = companies;
    }

    @Override
    public Optional<CompanyProfile> find(String companyCode) {
        if (companyCode == null) {
            return Optional.empty();
        }
        return companies.findById(companyCode).map(c -> new CompanyProfile(
                c.getCompanyCode(), c.getName(), c.getPresidentName(), c.getCorporateSecretaryName()));
    }
}
