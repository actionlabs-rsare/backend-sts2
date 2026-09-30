package com.ap.sts.company;

import com.ap.sts.company.CompanyDtos.CompanyInput;
import com.ap.sts.shared.audit.AuditService;
import com.ap.sts.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository repository;

    @Mock
    private AuditService audit;

    @InjectMocks
    private CompanyService service;

    @Test
    void createGeneratesFormattedCodeAndWritesAudit() {
        when(repository.nextCompanyCodeSeq()).thenReturn(4L);
        when(repository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanyInput input = new CompanyInput("Acme (SAMPLE)", "Manila", null, null, null, null, null, null);
        Company created = service.create(input);

        assertEquals("CO-004", created.getCompanyCode());
        ArgumentCaptor<String> ref = ArgumentCaptor.forClass(String.class);
        verify(audit).record(eq("company.create"), ref.capture(), isNull(), any());
        assertEquals("company/CO-004", ref.getValue());
    }

    @Test
    void getMissingThrowsNotFound() {
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get("CO-999"));
    }
}
