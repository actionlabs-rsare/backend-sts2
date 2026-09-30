package com.ap.sts.certificates.web;

import com.ap.sts.certificates.app.CertificateDocumentService;
import com.ap.sts.certificates.app.CertificateDocumentService.CertificateDocument;
import com.ap.sts.certificates.app.CertificatePrintService;
import com.ap.sts.certificates.app.CertificatePrintService.PrintCommand;
import com.ap.sts.certificates.domain.PrintMode;
import com.ap.sts.certificates.persistence.CertificateRepository;
import com.ap.sts.certificates.web.CertificateDtos.CertificateResponse;
import com.ap.sts.certificates.web.CertificateDtos.PrintRequest;
import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TR-017 / TR-018 certificate printing (contract path /certificates/print).
 * Print needs certificate:add (CR-S3-03 asks to grant it to User + Team Leader); a manual number
 * additionally needs Team Leader approval, checked server-side in CertificatePrintService.
 * The list and document endpoints are additive, pending contract change request CR-S3-C1.
 */
@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificatePrintService printing;
    private final CertificateDocumentService documents;
    private final CertificateRepository certificates;

    public CertificateController(CertificatePrintService printing, CertificateDocumentService documents,
                                 CertificateRepository certificates) {
        this.printing = printing;
        this.documents = documents;
        this.certificates = certificates;
    }

    @PostMapping("/print")
    @RequiresPermission(module = "certificate", action = Permission.add)
    public List<CertificateResponse> printCertificates(@Valid @RequestBody PrintRequest request) {
        PrintCommand command = new PrintCommand(request.transactionId(), request.mode(), request.manualNumber(),
                Boolean.TRUE.equals(request.teamLeaderApproval()));
        return printing.print(command).stream().map(CertificateResponse::from).toList();
    }

    /** Pending CR-S3-C1: certificates assigned to a transaction (used by S2's transaction detail). */
    @GetMapping
    @RequiresPermission(module = "certificate", action = Permission.list)
    public List<CertificateResponse> listCertificates(@RequestParam String transactionId) {
        Ids.requireValid(transactionId, "transactionId");
        return certificates.findByTransactionIdOrderByLineNo(transactionId).stream()
                .map(CertificateResponse::from).toList();
    }

    /** Pending CR-S3-C1: the printable PDF (TR-017). {@code mode} defaults to the mode used at print. */
    @GetMapping("/print/{transactionId}/document")
    @RequiresPermission(module = "certificate", action = Permission.view)
    public ResponseEntity<byte[]> getCertificateDocument(@PathVariable String transactionId,
                                                         @RequestParam(required = false) PrintMode mode) {
        Ids.requireValid(transactionId, "transactionId");
        CertificateDocument document = documents.render(transactionId, mode);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.fileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(document.content());
    }
}
