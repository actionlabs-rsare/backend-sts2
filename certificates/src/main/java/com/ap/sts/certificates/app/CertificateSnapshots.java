package com.ap.sts.certificates.app;

import com.ap.sts.certificates.domain.Certificate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Audit before/after snapshots of a certificate. Holder is recorded by stockholder CODE only,
 * never name or personal data (security-nfr §2, SECURITY-03).
 */
final class CertificateSnapshots {

    private CertificateSnapshots() {
    }

    static String ref(Certificate c) {
        return "certificate/" + c.getShareClassId() + "/" + c.getNumber();
    }

    static Map<String, Object> of(Certificate c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", c.getNumber());
        m.put("shareClassId", c.getShareClassId());
        m.put("holderStockholderCode", c.getHolderStockholderCode());
        m.put("shares", c.getShares());
        m.put("status", c.getStatus().name());
        m.put("origin", c.getOrigin().name());
        m.put("transactionId", c.getTransactionId());
        m.put("printMode", c.getPrintMode().name());
        m.put("manual", c.isManuallyAssigned());
        if (c.getApprovedBy() != null) {
            m.put("approvedBy", c.getApprovedBy());
        }
        return m;
    }
}
