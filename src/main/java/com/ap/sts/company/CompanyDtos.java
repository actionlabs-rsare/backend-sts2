package com.ap.sts.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Request/response DTOs matching companies.openapi.yaml. */
public final class CompanyDtos {

    private CompanyDtos() {
    }

    public record CompanyInput(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 300) String address,
            String tin,
            String secRegistrationCode,
            LocalDate incorporationDate,
            LocalDate annualMeetingDate,
            String presidentName,
            String corporateSecretaryName) {
    }

    public record CompanyResponse(
            String companyCode,
            String name,
            String address,
            String tin,
            String secRegistrationCode,
            LocalDate incorporationDate,
            LocalDate annualMeetingDate,
            String presidentName,
            String corporateSecretaryName) {

        public static CompanyResponse from(Company c) {
            return new CompanyResponse(
                    c.getCompanyCode(), c.getName(), c.getAddress(), c.getTin(),
                    c.getSecRegistrationCode(), c.getIncorporationDate(), c.getAnnualMeetingDate(),
                    c.getPresidentName(), c.getCorporateSecretaryName());
        }
    }
}
