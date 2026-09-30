package com.ap.sts.shares;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates and inspects the per-share-class certificate sequences (MDM-003, SL-002, TR-004).
 *
 * <p>Sequences are real database sequences, as the domain model requires, because S3 draws numbers
 * inside the posting transaction and must never hand out the same number twice. Two are created per
 * share class — original and replacement — both starting at 1, both independent of every other class.
 *
 * <p><b>Deployment note (raised as change request CR-7):</b> creating a sequence when a share class
 * is defined means the application role needs {@code CREATE} on the schema. That is broader than the
 * least-privilege grant described in `security-nfr.md` §3, so the grant needs a decision at merge:
 * either scope {@code CREATE} to a dedicated {@code numbering} schema, or pre-create a pool of
 * sequences by migration. Flagged rather than decided here.
 */
@Component
public class CertificateSequenceStore {

    private static final Logger log = LoggerFactory.getLogger(CertificateSequenceStore.class);

    private final JdbcTemplate jdbc;

    public CertificateSequenceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Creates this class's two sequences if they do not exist. Idempotent, so replaying a
     * create after a failed transaction cannot leave a share class without its numbering.
     */
    public void createFor(String shareClassId) {
        create(CertificateSequenceNames.original(shareClassId));
        create(CertificateSequenceNames.replacement(shareClassId));
    }

    private void create(String name) {
        // The name comes from CertificateSequenceNames, which validates it against a strict
        // identifier pattern — an identifier cannot be parameterised in DDL (SECURITY-05).
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS " + name + " START WITH 1 INCREMENT BY 1 NO CYCLE");
        log.info("certificate sequence ready name={}", name);
    }

    /**
     * The number the next certificate would get, without consuming it.
     *
     * <p>Returns {@code null} when the database cannot answer (the peek query is PostgreSQL-specific).
     * A display-only figure is not worth failing the whole request for, so callers render null as
     * "—" rather than a wrong number.
     */
    public Long peekNext(String sequenceName) {
        if (!CertificateSequenceNames.SAFE_NAME.matcher(sequenceName).matches()) {
            throw new IllegalArgumentException("Unsafe sequence name");
        }
        try {
            return jdbc.queryForObject(
                    "SELECT CASE WHEN is_called THEN last_value + 1 ELSE last_value END FROM "
                            + sequenceName,
                    Long.class);
        } catch (DataAccessException e) {
            log.warn("could not read sequence {} — reporting next number as unavailable", sequenceName);
            return null;
        }
    }
}
