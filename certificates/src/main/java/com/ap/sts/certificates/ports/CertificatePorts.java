package com.ap.sts.certificates.ports;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * What Unit S3 reads from other contexts, expressed as small ports with read models that mirror
 * the frozen contracts (transactions, shares, stockholders, companies). Wave 1 binds them to
 * synthetic fixtures or Wave 0 code; Wave 2 swaps the fixtures for the real units without
 * touching S3's services (never import another unit's code — construction rule 3).
 */
public final class CertificatePorts {

    private CertificatePorts() {
    }

    /** contracts/transactions.openapi.yaml → Transaction (status + transfer lines). */
    public interface TransactionSource {
        Optional<PrintableTransaction> find(String transactionId);
    }

    /** contracts/shares.openapi.yaml → ShareClass. */
    public interface ShareClassDirectory {
        Optional<ShareClassInfo> find(String shareClassId);
    }

    /** contracts/stockholders.openapi.yaml → Stockholder code, name and type (for the certificate face only). */
    public interface StockholderDirectory {
        Optional<StockholderRef> find(String stockholderCode);
    }

    /** contracts/companies.openapi.yaml → Company name + officers (TR-017 template). */
    public interface CompanyDirectory {
        Optional<CompanyProfile> find(String companyCode);
    }

    /** Status values follow contracts/transactions TransactionStatus. */
    public record PrintableTransaction(String id, String status, String companyCode, List<PrintableLine> lines) {
        public static final String APPROVED = "Approved";

        public boolean isApproved() {
            return APPROVED.equals(status);
        }
    }

    /** One transfer line = one new certificate to the transferee (S3-Q6). */
    public record PrintableLine(String fromStockholderCode, String toStockholderCode, String shareClassId,
                                long shares, String sourceCertificateNumber) {
    }

    public record ShareClassInfo(String id, String companyCode, String stockType, BigDecimal parValue) {
    }

    /** Stockholder.type follows the contract enum: "Individual" | "Corporate". */
    public record StockholderRef(String code, String name, String type) {
        public static final String CORPORATE = "Corporate";

        /** S3-Q9: certificates carry company names only, never an individual's name. */
        public boolean isCorporate() {
            return CORPORATE.equals(type);
        }
    }

    public record CompanyProfile(String companyCode, String name, String presidentName,
                                 String corporateSecretaryName) {
        public boolean hasOfficers() {
            return presidentName != null && !presidentName.isBlank()
                    && corporateSecretaryName != null && !corporateSecretaryName.isBlank();
        }
    }
}
