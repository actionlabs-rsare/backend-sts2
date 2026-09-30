package com.ap.sts.shares;

import com.ap.sts.shares.ShareClassDtos.ShareClassInput;
import com.ap.sts.shares.ShareClassDtos.ShareClassResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Mapping between the shares contract and the service. */
@ExtendWith(MockitoExtension.class)
class ShareClassControllerTest {

    @Mock
    private ShareClassService service;

    @InjectMocks
    private ShareClassController controller;

    private static ShareClass common() {
        ShareClass sc = new ShareClass("SC-000001", "CO-001", "Common", new BigDecimal("100.00"));
        sc.setAuthorizedShares(1_000_000L);
        return sc;
    }

    private static ShareClassResponse response() {
        return ShareClassResponse.from(common(), "1");
    }

    private static ShareClassInput input() {
        return new ShareClassInput("Common", new BigDecimal("100.00"), 1_000_000L, 0L, 0L, 0L);
    }

    @Test
    void listReturnsEachClassWithItsDerivedFields() {
        when(service.list("CO-001")).thenReturn(List.of(common()));
        when(service.response(any(ShareClass.class))).thenReturn(response());

        List<ShareClassResponse> result = controller.list("CO-001");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("100000000.00"), result.get(0).totalAmount());
        assertEquals("1", result.get(0).nextCertificateNumber());
    }

    @Test
    void createUsesTheCompanyCodeFromThePath() {
        when(service.create(eq("CO-001"), any())).thenReturn(common());
        when(service.response(any(ShareClass.class))).thenReturn(response());

        assertEquals("SC-000001", controller.create("CO-001", input()).id());
        verify(service).create(eq("CO-001"), any(ShareClassInput.class));
    }

    @Test
    void updateUsesTheIdFromThePath() {
        when(service.update(eq("SC-000001"), any())).thenReturn(common());
        when(service.response(any(ShareClass.class))).thenReturn(response());

        assertEquals("SC-000001", controller.update("CO-001", "SC-000001", input()).id());
        verify(service).update(eq("SC-000001"), any(ShareClassInput.class));
    }

    @Test
    void shareClassExposesItsCountsAndSequenceNames() {
        ShareClass sc = common();
        sc.setSubscribedShares(800_000L);
        sc.setIssuedShares(750_000L);
        sc.setTreasuryShares(1_000L);
        sc.setStockType("Common A");
        sc.setParValue(new BigDecimal("120.00"));

        assertEquals("CO-001", sc.getCompanyCode());
        assertEquals("Common A", sc.getStockType());
        assertEquals(new BigDecimal("120.00"), sc.getParValue());
        assertEquals(800_000L, sc.getSubscribedShares());
        assertEquals(750_000L, sc.getIssuedShares());
        assertEquals(1_000L, sc.getTreasuryShares());
        assertEquals("ShareClass[SC-000001]", sc.toString());
    }
}
