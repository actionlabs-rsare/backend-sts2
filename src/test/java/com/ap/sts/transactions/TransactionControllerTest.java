package com.ap.sts.transactions;

import com.ap.sts.approvals.ApprovalService;
import com.ap.sts.transactions.TransactionDtos.ApprovalStepView;
import com.ap.sts.transactions.TransactionDtos.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionControllerTest {

    private static final String ID = "TRF-000001";

    @Mock
    private TransferService transfers;
    @Mock
    private ApprovalService approvals;
    @Mock
    private PostingService posting;
    @Mock
    private TransactionReportService reports;

    private MockMvc mvc;

    private Transaction tx() {
        Transaction t = new Transaction(ID, TransactionType.Transfer, TransferType.Sale,
                "CO-001", LocalDate.of(2026, 9, 29), false);
        return t;
    }

    private TransactionResponse response(String status) {
        return new TransactionResponse(ID, "Transfer", status, "CO-001", LocalDate.of(2026, 9, 29),
                "Sale", "SRF-0001", "demo", false, Instant.now(), null,
                List.of(), List.of(),
                List.of(new ApprovalStepView(1, "FirstLevelApprover", "Pending", null, null, null)));
    }

    @BeforeEach
    void setUp() {
        TransactionController controller = new TransactionController(transfers, approvals, posting, reports);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(approvals.stepsFor(any())).thenReturn(List.of());
        when(transfers.toResponse(any(), any())).thenReturn(response("Submitted"));
    }

    @Test
    void listReturnsTransactions() throws Exception {
        when(transfers.list(any(), any(), any(), any())).thenReturn(List.of(tx()));
        mvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID));
    }

    @Test
    void createReturns201() throws Exception {
        when(transfers.createTransfer(any())).thenReturn(tx());
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("""
                {"companyCode":"CO-001","transactionDate":"2026-09-29","transferType":"Sale",
                 "lines":[{"fromStockholderCode":"SH-0001","toStockholderCode":"SH-0002",
                 "shareClassId":"SC-CO001-COMMON","shares":5000,"sourceCertificateNumber":"0142"}]}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    void createInvalidReturns422() throws Exception {
        when(transfers.createTransfer(any()))
                .thenThrow(new TransferValidationException("Cannot transfer more than held"));
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("""
                {"companyCode":"CO-001","transactionDate":"2026-09-29","transferType":"Sale",
                 "lines":[{"fromStockholderCode":"SH-0001","toStockholderCode":"SH-0002",
                 "shareClassId":"SC-CO001-COMMON","shares":999999,"sourceCertificateNumber":"0142"}]}
                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("transfer_validation_error"));
    }

    @Test
    void getReturnsTransaction() throws Exception {
        when(transfers.get(ID)).thenReturn(tx());
        mvc.perform(get("/api/transactions/{id}", ID)).andExpect(status().isOk());
    }

    @Test
    void submitApproveRejectAndPost() throws Exception {
        when(approvals.submit(ID)).thenReturn(tx());
        when(approvals.approve(eq(ID), any())).thenReturn(tx());
        when(approvals.reject(eq(ID), any())).thenReturn(tx());
        when(posting.markPrinted(ID)).thenReturn(tx());

        mvc.perform(post("/api/transactions/{id}/submit", ID)).andExpect(status().isOk());
        mvc.perform(post("/api/transactions/{id}/approve", ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"ok\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/transactions/{id}/reject", ID)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"docs incomplete\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/transactions/{id}/mark-printed", ID)).andExpect(status().isOk());
    }

    @Test
    void reportReturnsPdf() throws Exception {
        when(reports.generate(any(), any(), any(), any())).thenReturn("%PDF-1.4 test".getBytes());
        mvc.perform(get("/api/transactions/report").param("companyCode", "CO-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
