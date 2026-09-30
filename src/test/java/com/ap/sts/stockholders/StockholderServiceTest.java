package com.ap.sts.stockholders;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import com.ap.sts.stockholders.StockholderDtos.StockholderInput;
import com.ap.sts.stockholders.StockholderDtos.StockholderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** MDM-002 stockholder maintenance, plus the OI-19 "no record deletion" rule. */
@ExtendWith(MockitoExtension.class)
class StockholderServiceTest {

    @Mock
    private StockholderRepository repository;

    @Mock
    private FamilyGroupRepository familyGroups;

    @Mock
    private AuditService audit;

    @InjectMocks
    private StockholderService service;

    private static StockholderInput input() {
        return new StockholderInput("Santiago, Maria Clara (SAMPLE)", StockholderType.Individual,
                "123-456-789-000", "Filipino", Gender.Female, "mc.santiago@example.test",
                null, null, null, null, null);
    }

    @Test
    void createGeneratesCodeFromSequence() {
        when(repository.nextStockholderCodeSeq()).thenReturn(7L);
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        Stockholder created = service.create(input());

        assertEquals("SH-007", created.getStockholderCode());
        // MDM-002: the code comes from the stockholder's own sequence, not the company's.
        verify(repository).nextStockholderCodeSeq();
    }

    @Test
    void createWritesAuditWithNoBeforeState() {
        when(repository.nextStockholderCodeSeq()).thenReturn(1L);
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(input());

        ArgumentCaptor<String> ref = ArgumentCaptor.forClass(String.class);
        verify(audit).record(eq("stockholder.create"), ref.capture(), isNull(), any());
        assertEquals("stockholder/SH-001", ref.getValue());
    }

    @Test
    void createDefaultsToActive() {
        when(repository.nextStockholderCodeSeq()).thenReturn(2L);
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.create(input()).isActive());
    }

    @Test
    void createClearsCorporationTypeForIndividuals() {
        when(repository.nextStockholderCodeSeq()).thenReturn(3L);
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        StockholderInput individualWithCorpType = new StockholderInput(
                "Del Pilar, Marcelo (SAMPLE)", StockholderType.Individual, "333-444-555-000",
                "Filipino", Gender.Male, "m.delpilar@example.test", null, null,
                CorporationType.Domestic, null, null);

        assertNull(service.create(individualWithCorpType).getCorporationType());
    }

    @Test
    void createKeepsCorporationTypeForCorporates() {
        when(repository.nextStockholderCodeSeq()).thenReturn(4L);
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        StockholderInput corporate = new StockholderInput(
                "Rizal Holdings Corp. (SAMPLE)", StockholderType.Corporate, "222-333-444-000",
                "Filipino", Gender.PreferNotToSay, "corpsec@rizalholdings.example.test", null, null,
                CorporationType.Domestic, null, null);

        assertEquals(CorporationType.Domestic, service.create(corporate).getCorporationType());
    }

    @Test
    void createTreatsBlankOptionalsAsAbsent() {
        when(repository.nextStockholderCodeSeq()).thenReturn(5L);
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        StockholderInput blanks = new StockholderInput(
                "Bonifacio, Andres (SAMPLE)", StockholderType.Individual, "555-666-777-000",
                "Filipino", Gender.Male, "a.bonifacio@example.test", "  ", "  ", null, "  ", null);

        Stockholder created = service.create(blanks);
        assertNull(created.getMobile());
        assertNull(created.getAddress());
        assertNull(created.getFamilyGroupId());
    }

    @Test
    void createRejectsUnknownFamilyGroup() {
        when(familyGroups.existsById("FG-999")).thenReturn(false);

        StockholderInput orphan = new StockholderInput(
                "Luna, Antonio (SAMPLE)", StockholderType.Individual, "777-888-999-000",
                "Filipino", Gender.Male, "a.luna@example.test", null, null, null, "FG-999", null);

        assertThrows(NotFoundException.class, () -> service.create(orphan));
        verify(repository, never()).save(any());
    }

    @Test
    void createAcceptsKnownFamilyGroup() {
        when(familyGroups.existsById("FG-001")).thenReturn(true);
        when(repository.nextStockholderCodeSeq()).thenReturn(6L);
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        StockholderInput member = new StockholderInput(
                "Luna, Antonio (SAMPLE)", StockholderType.Individual, "777-888-999-000",
                "Filipino", Gender.Male, "a.luna@example.test", null, null, null, "FG-001", null);

        assertEquals("FG-001", service.create(member).getFamilyGroupId());
    }

    @Test
    void updateAuditsBeforeAndAfter() {
        Stockholder existing = seeded();
        when(repository.findById("SH-001")).thenReturn(Optional.of(existing));
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        StockholderInput renamed = new StockholderInput("Santiago-Reyes, Maria Clara (SAMPLE)",
                StockholderType.Individual, "123-456-789-000", "Filipino", Gender.Female,
                "mc.reyes@example.test", null, null, null, null, null);

        service.update("SH-001", renamed);

        ArgumentCaptor<Object> before = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> after = ArgumentCaptor.forClass(Object.class);
        verify(audit).record(eq("stockholder.update"), eq("stockholder/SH-001"),
                before.capture(), after.capture());
        // TR-006/013: the trail records what the value was as well as what it became.
        assertEquals("Santiago, Maria Clara (SAMPLE)",
                ((StockholderResponse) before.getValue()).name());
        assertEquals("Santiago-Reyes, Maria Clara (SAMPLE)",
                ((StockholderResponse) after.getValue()).name());
    }

    @Test
    void updateLeavesStatusUntouchedWhenNotSupplied() {
        Stockholder existing = seeded();
        existing.setActive(false);
        when(repository.findById("SH-001")).thenReturn(Optional.of(existing));
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        // active == null in the payload: an older contract client must not silently reactivate.
        assertFalse(service.update("SH-001", input()).isActive());
    }

    @Test
    void deactivateNotDelete() {
        Stockholder existing = seeded();
        when(repository.findById("SH-001")).thenReturn(Optional.of(existing));
        when(repository.save(any(Stockholder.class))).thenAnswer(inv -> inv.getArgument(0));

        Stockholder result = service.deactivate("SH-001");

        // OI-19 / Gate 3 Q4: the row survives; only its status changes, and the change is audited.
        assertFalse(result.isActive());
        assertEquals("SH-001", result.getStockholderCode());
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(anyString());
        verify(audit).record(eq("stockholder.deactivate"), eq("stockholder/SH-001"), any(), any());
    }

    @Test
    void getMissingThrowsNotFound() {
        when(repository.findById("SH-999")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get("SH-999"));
    }

    @Test
    void listWithoutQueryReturnsEverythingInAStableOrder() {
        when(repository.findAllOrdered()).thenReturn(List.of(seeded()));
        assertEquals(1, service.list(null).size());
        assertEquals(1, service.list("   ").size());
        // Not findAll(): unordered results reshuffle between refreshes.
        verify(repository, never()).findAll();
    }

    @Test
    void listWithQuerySearches() {
        when(repository.search("santiago")).thenReturn(List.of(seeded()));
        assertEquals(1, service.list("santiago").size());
    }

    private static Stockholder seeded() {
        Stockholder s = new Stockholder("SH-001", "Santiago, Maria Clara (SAMPLE)",
                StockholderType.Individual);
        s.setTin("123-456-789-000");
        s.setNationality("Filipino");
        s.setGender(Gender.Female);
        s.setEmail("mc.santiago@example.test");
        return s;
    }
}
