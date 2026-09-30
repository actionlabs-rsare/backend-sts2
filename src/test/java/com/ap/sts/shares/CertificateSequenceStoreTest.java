package com.ap.sts.shares;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Creation and inspection of the per-class certificate sequences (SL-002, TR-004). */
@ExtendWith(MockitoExtension.class)
class CertificateSequenceStoreTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private CertificateSequenceStore store;

    @Test
    void createsBothSeriesStartingAtOne() {
        store.createFor("SC-000001");

        ArgumentCaptor<String> ddl = ArgumentCaptor.forClass(String.class);
        verify(jdbc, org.mockito.Mockito.times(2)).execute(ddl.capture());
        List<String> statements = ddl.getAllValues();

        assertTrue(statements.get(0).contains("certificate_seq_sc_000001"));
        assertTrue(statements.get(1).contains("certificate_replacement_seq_sc_000001"));
        // "starting at 1" and "never cycles" are the two properties TR-004 depends on.
        assertTrue(statements.stream().allMatch(s -> s.contains("START WITH 1")));
        assertTrue(statements.stream().allMatch(s -> s.contains("NO CYCLE")));
        // Idempotent, so a retried create cannot leave a class without its numbering.
        assertTrue(statements.stream().allMatch(s -> s.contains("IF NOT EXISTS")));
    }

    @Test
    void peekReturnsTheNextNumberWithoutConsumingIt() {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(4L);

        assertEquals(4L, store.peekNext("certificate_seq_sc_000001"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(sql.capture(), eq(Long.class));
        // A peek must never call nextval: that would burn a certificate number just to show it.
        assertTrue(sql.getValue().toLowerCase().contains("last_value"));
        assertTrue(!sql.getValue().toLowerCase().contains("nextval"));
    }

    @Test
    void peekReportsUnavailableRatherThanFailingTheRequest() {
        when(jdbc.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new InvalidDataAccessResourceUsageException("no such sequence"));

        assertNull(store.peekNext("certificate_seq_sc_000001"));
    }

    @Test
    void peekRejectsAnUnsafeSequenceName() {
        assertThrows(IllegalArgumentException.class,
                () -> store.peekNext("certificate_seq_sc_000001; DROP TABLE share_class"));
    }
}
