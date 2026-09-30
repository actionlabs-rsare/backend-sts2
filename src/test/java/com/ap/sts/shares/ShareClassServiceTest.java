package com.ap.sts.shares;

import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import com.ap.sts.shares.ShareClassDtos.ShareClassInput;
import com.ap.sts.shares.ShareClassDtos.ShareClassResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** MDM-003 company share maintenance: computed totals and per-class numbering. */
@ExtendWith(MockitoExtension.class)
class ShareClassServiceTest {

    @Mock
    private ShareClassRepository repository;

    @Mock
    private CertificateSequenceStore sequences;

    @Mock
    private AuditService audit;

    @InjectMocks
    private ShareClassService service;

    private static ShareClassInput common() {
        // Matches mockups/company-shares.html: 1,000,000 authorized at ₱100 par = ₱100,000,000.00
        return new ShareClassInput("Common", new BigDecimal("100.00"),
                1_000_000L, 800_000L, 750_000L, 0L);
    }

    @Test
    void computesTotalAmount() {
        stubCreate(1L);

        ShareClass created = service.create("CO-001", common());

        assertEquals(new BigDecimal("100000000.00"), created.getTotalAmount());
    }

    @Test
    void totalAmountIsAuthorizedSharesTimesParValue() {
        stubCreate(2L);

        ShareClass created = service.create("CO-001",
                new ShareClassInput("Redeemable Preferred", new BigDecimal("50.00"),
                        200_000L, 120_000L, 120_000L, 5_000L));

        assertEquals(new BigDecimal("10000000.00"), created.getTotalAmount());
    }

    @Test
    void totalAmountKeepsCentavos() {
        stubCreate(3L);

        ShareClass created = service.create("CO-002",
                new ShareClassInput("Common", new BigDecimal("0.25"), 1_001L, 0L, 0L, 0L));

        assertEquals(new BigDecimal("250.25"), created.getTotalAmount());
    }

    @Test
    void createOpensThisClassOwnCertificateSequence() {
        stubCreate(4L);

        ShareClass created = service.create("CO-001", common());

        // SL-002: the sequence exists from the moment the class does, so S3 can always draw a number.
        verify(sequences).createFor("SC-000004");
        assertEquals("certificate_seq_sc_000004", created.getCertificateSequenceName());
        assertEquals("certificate_replacement_seq_sc_000004", created.getReplacementSequenceName());
    }

    @Test
    void eachClassGetsAnIndependentSequenceName() {
        ShareClass first = new ShareClass("SC-000001", "CO-001", "Common", new BigDecimal("100.00"));
        ShareClass second = new ShareClass("SC-000002", "CO-001", "Preferred", new BigDecimal("50.00"));

        // MDM-003: independent numbering per class/series — the names must never collide.
        assertNotEquals(first.getCertificateSequenceName(), second.getCertificateSequenceName());
        assertNotEquals(first.getCertificateSequenceName(), first.getReplacementSequenceName());
    }

    @Test
    void rejectsADuplicateStockTypeForTheSameCompany() {
        when(repository.findByCompanyAndStockType("CO-001", "Common"))
                .thenReturn(Optional.of(new ShareClass("SC-000001", "CO-001", "Common",
                        new BigDecimal("100.00"))));

        assertThrows(ShareClassConflictException.class, () -> service.create("CO-001", common()));
        verify(repository, never()).save(any());
        verify(sequences, never()).createFor(anyString());
    }

    @Test
    void rejectsSubscribedAboveAuthorized() {
        when(repository.findByCompanyAndStockType(anyString(), anyString())).thenReturn(Optional.empty());
        lenient().when(repository.nextShareClassIdSeq()).thenReturn(1L);

        ShareClassInput bad = new ShareClassInput("Common", new BigDecimal("100.00"),
                1_000L, 2_000L, 0L, 0L);

        assertEquals("Subscribed shares cannot exceed authorized shares",
                assertThrows(ShareClassConflictException.class,
                        () -> service.create("CO-001", bad)).getMessage());
    }

    @Test
    void rejectsIssuedAboveSubscribed() {
        when(repository.findByCompanyAndStockType(anyString(), anyString())).thenReturn(Optional.empty());
        lenient().when(repository.nextShareClassIdSeq()).thenReturn(1L);

        ShareClassInput bad = new ShareClassInput("Common", new BigDecimal("100.00"),
                1_000L, 500L, 800L, 0L);

        assertEquals("Issued shares cannot exceed subscribed shares",
                assertThrows(ShareClassConflictException.class,
                        () -> service.create("CO-001", bad)).getMessage());
    }

    @Test
    void rejectsTreasuryAboveIssued() {
        when(repository.findByCompanyAndStockType(anyString(), anyString())).thenReturn(Optional.empty());
        lenient().when(repository.nextShareClassIdSeq()).thenReturn(1L);

        ShareClassInput bad = new ShareClassInput("Common", new BigDecimal("100.00"),
                1_000L, 500L, 400L, 500L);

        assertEquals("Treasury shares cannot exceed issued shares",
                assertThrows(ShareClassConflictException.class,
                        () -> service.create("CO-001", bad)).getMessage());
    }

    @Test
    void omittedCountsDefaultToZero() {
        stubCreate(5L);

        ShareClass created = service.create("CO-003",
                new ShareClassInput("Common", new BigDecimal("25.00"), 500_000L, null, null, null));

        assertEquals(0L, created.getSubscribedShares());
        assertEquals(0L, created.getIssuedShares());
        assertEquals(0L, created.getTreasuryShares());
    }

    @Test
    void createIsAudited() {
        stubCreate(6L);

        service.create("CO-001", common());

        verify(audit).record(eq("shareClass.create"), eq("shareClass/SC-000006"), isNull(), any());
    }

    @Test
    void updateRecomputesTheTotalAndAuditsBeforeAndAfter() {
        ShareClass existing = new ShareClass("SC-000001", "CO-001", "Common", new BigDecimal("100.00"));
        existing.setAuthorizedShares(1_000_000L);
        when(repository.findById("SC-000001")).thenReturn(Optional.of(existing));
        when(repository.save(any(ShareClass.class))).thenAnswer(inv -> inv.getArgument(0));

        ShareClass updated = service.update("SC-000001",
                new ShareClassInput("Common", new BigDecimal("120.00"), 1_000_000L, 0L, 0L, 0L));

        assertEquals(new BigDecimal("120000000.00"), updated.getTotalAmount());
        verify(audit).record(eq("shareClass.update"), eq("shareClass/SC-000001"), any(), any());
    }

    @Test
    void updateOfAnUnknownClassIsNotFound() {
        when(repository.findById("SC-999999")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> service.update("SC-999999", common()));
    }

    @Test
    void listDelegatesToTheCompanyQuery() {
        when(repository.findByCompanyCode("CO-001")).thenReturn(List.of(
                new ShareClass("SC-000001", "CO-001", "Common", new BigDecimal("100.00"))));

        assertEquals(1, service.list("CO-001").size());
    }

    @Test
    void responseCarriesTheNextCertificateNumber() {
        ShareClass shareClass = new ShareClass("SC-000001", "CO-001", "Common", new BigDecimal("100.00"));
        when(sequences.peekNext("certificate_seq_sc_000001")).thenReturn(1L);

        ShareClassResponse response = service.response(shareClass);

        // Starts at 1 for a brand-new class (MDM-003 "sequences starting at 1").
        assertEquals("1", response.nextCertificateNumber());
    }

    @Test
    void responseReportsAnUnreadableSequenceAsAbsent() {
        ShareClass shareClass = new ShareClass("SC-000001", "CO-001", "Common", new BigDecimal("100.00"));
        when(sequences.peekNext("certificate_seq_sc_000001")).thenReturn(null);

        assertNull(service.response(shareClass).nextCertificateNumber());
    }

    @Test
    void parValueDefaultsToZeroTotalWhenAbsent() {
        ShareClass shareClass = new ShareClass("SC-000001", "CO-001", "Common", null);
        assertEquals(new BigDecimal("0.00"), shareClass.getTotalAmount());
    }

    private void stubCreate(long nextId) {
        when(repository.findByCompanyAndStockType(anyString(), anyString())).thenReturn(Optional.empty());
        when(repository.nextShareClassIdSeq()).thenReturn(nextId);
        when(repository.save(any(ShareClass.class))).thenAnswer(inv -> inv.getArgument(0));
    }
}
