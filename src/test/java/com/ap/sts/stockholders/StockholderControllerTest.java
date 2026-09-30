package com.ap.sts.stockholders;

import com.ap.sts.stockholders.StockholderDtos.FamilyGroupInput;
import com.ap.sts.stockholders.StockholderDtos.StockholderInput;
import com.ap.sts.stockholders.StockholderDtos.StockholderResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The S1 controllers map between the contract DTOs and the services. These tests check the
 * mapping — in particular that responses carry exactly what the contract promises, and that
 * {@code sharesHeld} stays absent outside the family-group inquiry.
 */
@ExtendWith(MockitoExtension.class)
class StockholderControllerTest {

    @Mock
    private StockholderService stockholderService;

    @Mock
    private FamilyGroupService familyGroupService;

    @InjectMocks
    private StockholderController controller;

    private static StockholderInput input() {
        return new StockholderInput("Santiago, Maria Clara (SAMPLE)", StockholderType.Individual,
                "123-456-789-000", "Filipino", Gender.Female, "mc.santiago@example.test",
                null, null, null, "FG-001", null);
    }

    private static Stockholder seeded() {
        Stockholder s = new Stockholder("SH-001", "Santiago, Maria Clara (SAMPLE)",
                StockholderType.Individual);
        s.setTin("123-456-789-000");
        s.setNationality("Filipino");
        s.setGender(Gender.Female);
        s.setEmail("mc.santiago@example.test");
        s.setFamilyGroupId("FG-001");
        return s;
    }

    @Test
    void listPassesTheSearchTermThrough() {
        when(stockholderService.list("santiago")).thenReturn(List.of(seeded()));

        List<StockholderResponse> response = controller.list("santiago");

        assertEquals(1, response.size());
        assertEquals("SH-001", response.get(0).stockholderCode());
    }

    @Test
    void listDoesNotReportShareholdings() {
        when(stockholderService.list(null)).thenReturn(List.of(seeded()));


        // sharesHeld belongs to the family-group inquiry; null here means "not part of this view".
        assertNull(controller.list(null).get(0).sharesHeld());
    }

    @Test
    void getMapsEveryContractField() {
        when(stockholderService.get("SH-001")).thenReturn(seeded());

        StockholderResponse response = controller.get("SH-001");

        assertEquals("SH-001", response.stockholderCode());
        assertEquals(StockholderType.Individual, response.type());
        assertEquals("123-456-789-000", response.tin());
        assertEquals("Filipino", response.nationality());
        assertEquals(Gender.Female, response.gender());
        assertEquals("mc.santiago@example.test", response.email());
        assertEquals("FG-001", response.familyGroupId());
    }

    @Test
    void createDelegatesAndReturnsTheStoredRecord() {
        when(stockholderService.create(any())).thenReturn(seeded());

        assertEquals("SH-001", controller.create(input()).stockholderCode());
        verify(stockholderService).create(any(StockholderInput.class));
    }

    @Test
    void updateDelegatesWithTheCodeFromThePath() {
        when(stockholderService.update(eq("SH-001"), any())).thenReturn(seeded());

        assertEquals("SH-001", controller.update("SH-001", input()).stockholderCode());
        verify(stockholderService).update(eq("SH-001"), any(StockholderInput.class));
    }

    @Test
    void familyGroupControllerListsAndCreates() {
        FamilyGroupController groups = new FamilyGroupController(familyGroupService);
        when(familyGroupService.list())
                .thenReturn(List.of(new FamilyGroup("FG-001", "Santiago family (SAMPLE)")));
        when(familyGroupService.create("Luna family (SAMPLE)"))
                .thenReturn(new FamilyGroup("FG-003", "Luna family (SAMPLE)"));

        assertEquals("FG-001", groups.list().get(0).id());
        assertEquals("FG-003", groups.create(new FamilyGroupInput("Luna family (SAMPLE)")).id());
    }

    @Test
    void familyGroupControllerReturnsMembersWithTheirShareholdings() {
        FamilyGroupController groups = new FamilyGroupController(familyGroupService);
        when(familyGroupService.members("FG-001"))
                .thenReturn(List.of(StockholderResponse.from(seeded(), 1_500L)));

        List<StockholderResponse> members = groups.members("FG-001");

        assertEquals(1, members.size());
        assertEquals(1_500L, members.get(0).sharesHeld());
    }
}
