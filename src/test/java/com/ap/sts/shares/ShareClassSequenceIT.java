package com.ap.sts.shares;

import com.ap.sts.shares.ShareClassDtos.ShareClassInput;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MDM-003 / SL-002 against a real PostgreSQL: every share class gets its own certificate-number
 * sequence starting at 1, and drawing from one class never disturbs another.
 *
 * <p>Skipped when Docker is unavailable; CI runs it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class ShareClassSequenceIT {

    @Container
    @SuppressWarnings("resource") // lifecycle is managed by the Testcontainers extension
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sts")
            .withUsername("sts")
            .withPassword("sts");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ShareClassService service;

    @Autowired
    private CertificateSequenceStore sequences;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void independentSequencePerClass() {
        ShareClass preferred = service.create("CO-002",
                new ShareClassInput("Preferred", new BigDecimal("40.00"), 100_000L, 0L, 0L, 0L));
        ShareClass redeemable = service.create("CO-002",
                new ShareClassInput("Redeemable Preferred", new BigDecimal("60.00"),
                        50_000L, 0L, 0L, 0L));

        // MDM-003: "independent certificate numbering sequences starting at 1" for each class.
        assertEquals(1L, sequences.peekNext(preferred.getCertificateSequenceName()));
        assertEquals(1L, sequences.peekNext(redeemable.getCertificateSequenceName()));

        // Drawing two numbers from one class (as S3 will on printing)...
        jdbc.queryForObject("select nextval('" + preferred.getCertificateSequenceName() + "')",
                Long.class);
        jdbc.queryForObject("select nextval('" + preferred.getCertificateSequenceName() + "')",
                Long.class);

        // ...advances only that class.
        assertEquals(3L, sequences.peekNext(preferred.getCertificateSequenceName()));
        assertEquals(1L, sequences.peekNext(redeemable.getCertificateSequenceName()));
    }

    @Test
    void replacementNumbersComeFromASeparateSeries() {
        ShareClass shareClass = service.create("CO-003",
                new ShareClassInput("Preferred", new BigDecimal("15.00"), 10_000L, 0L, 0L, 0L));

        jdbc.queryForObject("select nextval('" + shareClass.getCertificateSequenceName() + "')",
                Long.class);

        // TR-004: the replacement series is untouched by the original series, so a replacement
        // number can never be one that was already issued.
        assertEquals(2L, sequences.peekNext(shareClass.getCertificateSequenceName()));
        assertEquals(1L, sequences.peekNext(shareClass.getReplacementSequenceName()));
    }

    @Test
    void seededClassesAlreadyHaveTheirSequences() {
        assertEquals(1L, sequences.peekNext("certificate_seq_sc_000001"));
        assertEquals(1L, sequences.peekNext("certificate_replacement_seq_sc_000001"));
        assertEquals(1L, sequences.peekNext("certificate_seq_sc_000002"));
    }

    @Test
    void theDatabaseAlsoRefusesCountsThatDoNotNest() {
        // The service checks this, and so does a CHECK constraint — the rule holds even if a future
        // code path writes the row directly.
        assertThrows(Exception.class, () -> jdbc.update(
                "insert into share_class (id, company_code, stock_type, par_value, "
                        + "authorized_shares, subscribed_shares, issued_shares, treasury_shares) "
                        + "values ('SC-999999', 'CO-001', 'Bad', 1.0, 100, 200, 0, 0)"));
    }

    @Test
    void duplicateStockTypePerCompanyIsRefusedByTheDatabaseToo() {
        assertThrows(Exception.class, () -> jdbc.update(
                "insert into share_class (id, company_code, stock_type, par_value, "
                        + "authorized_shares, subscribed_shares, issued_shares, treasury_shares) "
                        + "values ('SC-999998', 'CO-001', 'Common', 1.0, 100, 0, 0, 0)"));
    }
}
