package com.ap.sts.stockholders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.stockholders.StockholderDtos.StockholderInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * T7 (information disclosure): stockholder personal data must never reach a log line.
 *
 * <p>This test captures everything {@link StockholderService} logs while creating and updating a
 * record, then asserts that none of the personal values appear. It is the mechanical check behind
 * the "mask personal data in logs" control, so a future log statement that leaks a TIN fails here
 * rather than in production.
 */
@ExtendWith(MockitoExtension.class)
class StockholderLoggingTest {

    private static final String TIN = "123-456-789-000";
    private static final String EMAIL = "mc.santiago@example.test";
    private static final String MOBILE = "+63 900 000 0001";
    private static final String ADDRESS = "Cebu City, PH";

    @Mock
    private StockholderRepository repository;

    @Mock
    private FamilyGroupRepository familyGroups;

    @Mock
    private AuditService audit;

    private StockholderService service;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        service = new StockholderService(repository, familyGroups, audit);
        logger = (Logger) LoggerFactory.getLogger(StockholderService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    private String captured() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private static StockholderInput input() {
        return new StockholderInput("Santiago, Maria Clara (SAMPLE)", StockholderType.Individual,
                TIN, "Filipino", Gender.Female, EMAIL, MOBILE, ADDRESS, null, null, null);
    }

    @Test
    void noPiiInLogs() {
        when(repository.nextStockholderCodeSeq()).thenReturn(1L);
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(input());
        String logs = captured();

        assertFalse(logs.contains(TIN), "the TIN must not be logged: " + logs);
        assertFalse(logs.contains(EMAIL), "the email must not be logged: " + logs);
        assertFalse(logs.contains(MOBILE), "the mobile number must not be logged: " + logs);
        assertFalse(logs.contains(ADDRESS), "the address must not be logged: " + logs);
        assertFalse(logs.contains("Santiago"), "the person's name must not be logged: " + logs);
    }

    @Test
    void logsTheCodeSoAnEventIsStillTraceable() {
        when(repository.nextStockholderCodeSeq()).thenReturn(1L);
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(input());

        // Masking is worthless if it also removes the ability to investigate: the code stays.
        assertTrue(captured().contains("SH-001"));
        assertTrue(captured().contains("***-000"), "the masked TIN tail aids support triage");
    }

    @Test
    void updateAndDeactivateAlsoStayClean() {
        Stockholder existing = new Stockholder("SH-001", "Santiago, Maria Clara (SAMPLE)",
                StockholderType.Individual);
        existing.setTin(TIN);
        existing.setNationality("Filipino");
        existing.setGender(Gender.Female);
        existing.setEmail(EMAIL);
        when(repository.findById("SH-001")).thenReturn(Optional.of(existing));
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update("SH-001", input());
        service.deactivate("SH-001");

        String logs = captured();
        assertFalse(logs.contains(TIN));
        assertFalse(logs.contains(EMAIL));
        assertTrue(logs.contains("SH-001"));
    }

    @Test
    void entityToStringCarriesNoPersonalData() {
        Stockholder s = new Stockholder("SH-002", "Rizal Holdings Corp. (SAMPLE)",
                StockholderType.Corporate);
        s.setTin(TIN);
        s.setEmail(EMAIL);

        // A stray log.info("... {}", stockholder) is the most likely way PII escapes, so the
        // entity's own toString is deliberately code-only.
        assertTrue(s.toString().contains("SH-002"));
        assertFalse(s.toString().contains(TIN));
        assertFalse(s.toString().contains(EMAIL));
        assertFalse(s.toString().contains("Rizal"));
    }
}
