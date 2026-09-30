package com.ap.sts.certificates.persistence;

import com.ap.sts.certificates.domain.CertificateOrigin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin JDBC access to the S3 counter functions (migration V202609290300). All calls join the
 * caller's transaction. The database — not this class — enforces the numbering rules: the counter
 * moves +1 per number inside the transaction (gapless, S3-Q7), a starting point can't change once
 * a series has issued, and nothing can be rewound.
 */
@Component
public class SequenceGateway {

    /** Advisory-lock key spaces (int4 pairs): 3 = numbering series, 4 = prints per transaction. */
    static final int SERIES_LOCK_SPACE = 3;
    static final int PRINT_LOCK_SPACE = 4;

    private final JdbcTemplate jdbc;

    public SequenceGateway(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Serialises creation/configuration of one series until the current transaction ends. */
    public void lockSeries(String shareClassId, CertificateOrigin origin) {
        jdbc.queryForObject("select 1 from (select pg_advisory_xact_lock(?, hashtext(?))) l", Integer.class,
                SERIES_LOCK_SPACE, shareClassId + "|" + origin.name());
    }

    /** Next value the counter will hand out (1 for a series that doesn't exist yet). Read-only. */
    public long peekNext(String shareClassId, CertificateOrigin origin) {
        Long v = jdbc.queryForObject("select certificates_peek_next(?, ?)", Long.class, shareClassId, origin.name());
        return v == null ? 1L : v;
    }

    /** Takes the next number (counter +1, row-locked until commit; a rollback gives it back). */
    public long next(String shareClassId, CertificateOrigin origin) {
        Long v = jdbc.queryForObject("select certificates_next_number(?, ?)", Long.class, shareClassId, origin.name());
        if (v == null) {
            throw new IllegalStateException("certificate counter returned no value");
        }
        return v;
    }

    /** Sets the starting point; the DB refuses once the series has issued (gapless). */
    public void setStart(String shareClassId, CertificateOrigin origin, long start) {
        jdbc.queryForObject("select certificates_set_start(?, ?, ?)", Long.class, shareClassId, origin.name(), start);
    }

    /** Serialises prints of the same transaction until the current transaction ends. */
    public void lockTransactionForPrint(String transactionId) {
        jdbc.queryForObject("select 1 from (select pg_advisory_xact_lock(?, hashtext(?))) l", Integer.class,
                PRINT_LOCK_SPACE, transactionId);
    }
}
