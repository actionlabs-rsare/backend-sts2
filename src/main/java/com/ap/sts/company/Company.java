package com.ap.sts.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Company aggregate (MDM-001). companyCode is assigned from a dedicated DB sequence. */
@Entity
@Table(name = "company")
public class Company {

    @Id
    @Column(name = "company_code", nullable = false, updatable = false)
    private String companyCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String tin;

    @Column(name = "sec_registration_code")
    private String secRegistrationCode;

    @Column(name = "incorporation_date")
    private LocalDate incorporationDate;

    @Column(name = "annual_meeting_date")
    private LocalDate annualMeetingDate;

    @Column(name = "president_name")
    private String presidentName;

    @Column(name = "corporate_secretary_name")
    private String corporateSecretaryName;

    protected Company() {
        // JPA
    }

    public Company(String companyCode, String name, String address) {
        this.companyCode = companyCode;
        this.name = name;
        this.address = address;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTin() {
        return tin;
    }

    public void setTin(String tin) {
        this.tin = tin;
    }

    public String getSecRegistrationCode() {
        return secRegistrationCode;
    }

    public void setSecRegistrationCode(String secRegistrationCode) {
        this.secRegistrationCode = secRegistrationCode;
    }

    public LocalDate getIncorporationDate() {
        return incorporationDate;
    }

    public void setIncorporationDate(LocalDate incorporationDate) {
        this.incorporationDate = incorporationDate;
    }

    public LocalDate getAnnualMeetingDate() {
        return annualMeetingDate;
    }

    public void setAnnualMeetingDate(LocalDate annualMeetingDate) {
        this.annualMeetingDate = annualMeetingDate;
    }

    public String getPresidentName() {
        return presidentName;
    }

    public void setPresidentName(String presidentName) {
        this.presidentName = presidentName;
    }

    public String getCorporateSecretaryName() {
        return corporateSecretaryName;
    }

    public void setCorporateSecretaryName(String corporateSecretaryName) {
        this.corporateSecretaryName = corporateSecretaryName;
    }
}
