package com.ap.sts.stockholders;

import com.ap.sts.company.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MDM-002 against a real PostgreSQL: a stockholder is a separate entity from a company, drawing its
 * code from its own sequence. Runs on Aurora's engine version (PG 16) so the sequence and constraint
 * behaviour matches production rather than an in-memory approximation.
 *
 * <p>Skipped automatically when Docker is unavailable, so a developer without a daemon still gets a
 * green unit-test run; CI has Docker and runs it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class StockholderRepositoryIT {

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
    private StockholderRepository stockholders;

    @Autowired
    private CompanyRepository companies;

    @Autowired
    private StockholderService service;

    @Test
    void stockholderIndependentOfCompany() {
        long companyBefore = companies.nextCompanyCodeSeq();

        long first = stockholders.nextStockholderCodeSeq();
        long second = stockholders.nextStockholderCodeSeq();

        // The stockholder sequence advances on its own...
        assertEquals(first + 1, second);
        // ...and moving it does not move the company sequence (MDM-002: separate entities).
        assertEquals(companyBefore + 1, companies.nextCompanyCodeSeq());
    }

    @Test
    void listIsReturnedInCodeOrder() {
        List<String> codes = stockholders.findAllOrdered().stream()
                .map(Stockholder::getStockholderCode)
                .toList();

        assertEquals(codes.stream().sorted().toList(), codes,
                "the maintenance list must not reshuffle between refreshes");
    }

    @Test
    void seededStockholdersLoadWithTheirFamilyGroups() {
        List<Stockholder> all = stockholders.findAllOrdered();

        assertTrue(all.size() >= 5, "expected the synthetic seed to load");
        Stockholder maria = stockholders.findById("SH-001").orElseThrow();
        assertEquals("FG-001", maria.getFamilyGroupId());
        assertEquals(Gender.Female, maria.getGender());
        assertNotNull(maria.getTin());
    }

    @Test
    void familyGroupMembershipIsQueryable() {
        List<Stockholder> group = stockholders.findByFamilyGroupId("FG-002");

        assertTrue(group.size() >= 2);
        assertTrue(group.stream().allMatch(s -> "FG-002".equals(s.getFamilyGroupId())));
    }

    @Test
    void searchMatchesCodeAndName() {
        assertFalse(stockholders.search("SH-001").isEmpty());
        assertFalse(stockholders.search("santiago").isEmpty());
    }

    @Test
    void deactivatedStockholdersStayInTheTable() {
        // OI-19: SH-005 is seeded inactive, and it is still there to be read and reported on.
        Stockholder retired = stockholders.findById("SH-005").orElseThrow();
        assertFalse(retired.isActive());
    }

    @Test
    void createPersistsThroughTheServiceWithAGeneratedCode() {
        Stockholder created = service.create(new StockholderDtos.StockholderInput(
                "Jacinto, Emilio (SAMPLE)", StockholderType.Individual, "888-999-000-111",
                "Filipino", Gender.Male, "e.jacinto@example.test", null, null, null, null, null));

        assertTrue(created.getStockholderCode().startsWith("SH-"));
        assertTrue(stockholders.findById(created.getStockholderCode()).isPresent());
    }
}
