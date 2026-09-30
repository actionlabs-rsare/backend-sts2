package com.ap.sts.company;

import com.ap.sts.company.CompanyDtos.CompanyInput;
import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository repository;
    private final AuditService audit;

    public CompanyService(CompanyRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Company> list(String q) {
        if (q == null || q.isBlank()) {
            return repository.findAll();
        }
        return repository.search(q);
    }

    @Transactional(readOnly = true)
    public Company get(String code) {
        return repository.findById(code)
                .orElseThrow(() -> new NotFoundException("Company not found: " + code));
    }

    @Transactional
    public Company create(CompanyInput input) {
        String code = String.format("CO-%03d", repository.nextCompanyCodeSeq());
        Company company = new Company(code, input.name(), input.address());
        apply(company, input);
        Company saved = repository.save(company);
        audit.record("company.create", "company/" + code, null, CompanyDtos.CompanyResponse.from(saved));
        return saved;
    }

    @Transactional
    public Company update(String code, CompanyInput input) {
        Company company = get(code);
        var before = CompanyDtos.CompanyResponse.from(company);
        // NOTE: Name/Address changes require an SEC-approved amendment (TR-009) — deferred.
        apply(company, input);
        company.setName(input.name());
        company.setAddress(input.address());
        Company saved = repository.save(company);
        audit.record("company.update", "company/" + code, before, CompanyDtos.CompanyResponse.from(saved));
        return saved;
    }

    private void apply(Company company, CompanyInput input) {
        company.setTin(input.tin());
        company.setSecRegistrationCode(input.secRegistrationCode());
        company.setIncorporationDate(input.incorporationDate());
        company.setAnnualMeetingDate(input.annualMeetingDate());
        company.setPresidentName(input.presidentName());
        company.setCorporateSecretaryName(input.corporateSecretaryName());
    }
}
