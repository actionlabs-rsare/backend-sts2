package com.ap.sts.transactions;

import com.ap.sts.approvals.ApprovalService;
import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.RequiresPermission;
import com.ap.sts.shared.error.ApiError;
import com.ap.sts.transactions.TransactionDtos.DecisionInput;
import com.ap.sts.transactions.TransactionDtos.TransactionResponse;
import com.ap.sts.transactions.TransactionDtos.TransferInput;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Transfer + approval + posting + report API (TR-003, SL-004, TR-012, TR-013, IR-TXN),
 * matching transactions.openapi.yaml. Every mutating step is server-side and role-checked
 * (@RequiresPermission + per-transition checks in ApprovalService).
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransferService transfers;
    private final ApprovalService approvals;
    private final PostingService posting;
    private final TransactionReportService reports;

    public TransactionController(TransferService transfers, ApprovalService approvals,
                                 PostingService posting, TransactionReportService reports) {
        this.transfers = transfers;
        this.approvals = approvals;
        this.posting = posting;
        this.reports = reports;
    }

    @GetMapping
    @RequiresPermission(module = "transaction", action = Permission.list)
    public List<TransactionResponse> list(@RequestParam(required = false) TransactionType type,
                                          @RequestParam(required = false) String companyCode,
                                          @RequestParam(required = false) TransactionStatus status) {
        return transfers.list(companyCode, status, null, null).stream()
                .filter(t -> type == null || t.getType() == type)
                .map(this::view)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(module = "transaction", action = Permission.add)
    public TransactionResponse createTransfer(@Valid @RequestBody TransferInput input) {
        return view(transfers.createTransfer(input));
    }

    @GetMapping("/report")
    @RequiresPermission(module = "transaction", action = Permission.view)
    public ResponseEntity<byte[]> report(
            @RequestParam(required = false) String companyCode,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        byte[] pdf = reports.generate(companyCode, status, dateFrom, dateTo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transaction-report.pdf\"")
                .body(pdf);
    }

    @GetMapping("/{id}")
    @RequiresPermission(module = "transaction", action = Permission.view)
    public TransactionResponse get(@PathVariable String id) {
        return view(transfers.get(id));
    }

    @PostMapping("/{id}/submit")
    @RequiresPermission(module = "transaction", action = Permission.add)
    public TransactionResponse submit(@PathVariable String id) {
        return view(approvals.submit(id));
    }

    @PostMapping("/{id}/approve")
    @RequiresPermission(module = "transaction", action = Permission.approve)
    public TransactionResponse approve(@PathVariable String id,
                                       @RequestBody(required = false) DecisionInput body) {
        return view(approvals.approve(id, body == null ? null : body.reason()));
    }

    @PostMapping("/{id}/reject")
    @RequiresPermission(module = "transaction", action = Permission.approve)
    public TransactionResponse reject(@PathVariable String id,
                                      @RequestBody(required = false) DecisionInput body) {
        return view(approvals.reject(id, body == null ? null : body.reason()));
    }

    @PostMapping("/{id}/mark-printed")
    @RequiresPermission(module = "transaction", action = Permission.post)
    public TransactionResponse markPrinted(@PathVariable String id) {
        return view(posting.markPrinted(id));
    }

    private TransactionResponse view(Transaction tx) {
        return transfers.toResponse(tx, approvals.stepsFor(tx.getId()));
    }

    /**
     * Controller-local 422 for business-rule breaches (share conservation, illegal transitions).
     * A handler declared on the controller takes precedence over the shared @RestControllerAdvice,
     * whose catch-all would otherwise map these to 500.
     */
    @ExceptionHandler(TransferValidationException.class)
    public ResponseEntity<ApiError> handleTransferValidation(TransferValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of("transfer_validation_error", ex.getMessage()));
    }
}
