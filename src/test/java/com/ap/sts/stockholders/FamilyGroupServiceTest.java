package com.ap.sts.stockholders;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import com.ap.sts.stockholders.StockholderDtos.StockholderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** MDM-002-FG family groups and the grouping inquiry (OI-15). */
@ExtendWith(MockitoExtension.class)
class FamilyGroupServiceTest {

    @Mock
    private FamilyGroupRepository repository;

    @Mock
    private StockholderRepository stockholders;

    @Mock
    private HoldingsLookup holdings;

    @Mock
    private AuditService audit;

    @InjectMocks
    private FamilyGroupService service;

    @Test
    void createGeneratesIdFromItsOwnSequenceAndAudits() {
        when(repository.nextFamilyGroupIdSeq()).thenReturn(3L);
        when(repository.save(any(FamilyGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyGroup created = service.create("Santiago family (SAMPLE)");

        assertEquals("FG-003", created.getId());
        verify(audit).record(eq("familyGroup.create"), eq("familyGroup/FG-003"), isNull(), any());
    }

    @Test
    void groupsCombinedHoldings() {
        when(repository.existsById("FG-001")).thenReturn(true);
        when(stockholders.findByFamilyGroupId("FG-001")).thenReturn(List.of(
                member("SH-001", "Santiago, Maria Clara (SAMPLE)"),
                member("SH-006", "Luna, Antonio (SAMPLE)")));
        when(holdings.sharesHeldBy("SH-001")).thenReturn(1_500L);
        when(holdings.sharesHeldBy("SH-006")).thenReturn(500L);

        List<StockholderResponse> members = service.members("FG-001");

        assertEquals(2, members.size());
        assertEquals(1_500L, members.get(0).sharesHeld());
        assertEquals(500L, members.get(1).sharesHeld());
        // The inquiry's purpose: shareholdings can be totalled per group.
        assertEquals(2_000L, members.stream().mapToLong(StockholderResponse::sharesHeld).sum());
    }

    @Test
    void unknownHoldingsStayNullRatherThanZero() {
        when(repository.existsById("FG-002")).thenReturn(true);
        when(stockholders.findByFamilyGroupId("FG-002"))
                .thenReturn(List.of(member("SH-003", "Del Pilar, Marcelo (SAMPLE)")));
        when(holdings.sharesHeldBy("SH-003")).thenReturn(null);

        // Wave 1 has no holding ledger yet. Reporting 0 would be a wrong number, not a missing one.
        assertNull(service.members("FG-002").get(0).sharesHeld());
    }

    @Test
    void membersOfAnUnknownGroupIsNotFound() {
        when(repository.existsById("FG-999")).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.members("FG-999"));
    }

    @Test
    void listReturnsGroupsInNameOrder() {
        when(repository.findAllOrdered()).thenReturn(List.of(
                new FamilyGroup("FG-002", "Del Pilar family (SAMPLE)"),
                new FamilyGroup("FG-001", "Santiago family (SAMPLE)")));

        assertEquals(List.of("Del Pilar family (SAMPLE)", "Santiago family (SAMPLE)"),
                service.list().stream().map(FamilyGroup::getName).toList());
    }

    @Test
    void groupNameIsEditable() {
        FamilyGroup group = new FamilyGroup("FG-001", "Santiago family (SAMPLE)");
        group.setName("Santiago-Reyes family (SAMPLE)");
        assertEquals("Santiago-Reyes family (SAMPLE)", group.getName());
    }

    private static Stockholder member(String code, String name) {
        Stockholder s = new Stockholder(code, name, StockholderType.Individual);
        s.setTin("000-000-000-000");
        s.setNationality("Filipino");
        s.setGender(Gender.PreferNotToSay);
        s.setEmail("member@example.test");
        s.setFamilyGroupId("FG-001");
        return s;
    }
}
