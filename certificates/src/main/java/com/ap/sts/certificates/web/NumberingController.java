package com.ap.sts.certificates.web;

import com.ap.sts.certificates.app.NumberingService;
import com.ap.sts.certificates.app.NumberingService.NumberingChange;
import com.ap.sts.certificates.domain.CertificateRuleException;
import com.ap.sts.certificates.web.CertificateDtos.NumberingConfig;
import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SL-002 Stock Certificate Configuration (contract paths /share-classes/{shareClassId}/numbering).
 * Reading needs certificate:view; changing starting points needs certificate:change (Admin, per SL-002).
 */
@RestController
@RequestMapping("/api/share-classes/{shareClassId}/numbering")
public class NumberingController {

    private final NumberingService numbering;

    public NumberingController(NumberingService numbering) {
        this.numbering = numbering;
    }

    @GetMapping
    @RequiresPermission(module = "certificate", action = Permission.view)
    public NumberingConfig getNumbering(@PathVariable String shareClassId) {
        Ids.requireValid(shareClassId, "shareClassId");
        return NumberingConfig.from(numbering.view(shareClassId));
    }

    @PutMapping
    @RequiresPermission(module = "certificate", action = Permission.change)
    public NumberingConfig setNumbering(@PathVariable String shareClassId, @Valid @RequestBody NumberingConfig body) {
        Ids.requireValid(shareClassId, "shareClassId");
        if (body.shareClassId() != null && !body.shareClassId().equals(shareClassId)) {
            throw new CertificateRuleException("validation_error",
                    "The body is for share class " + body.shareClassId() + " but the address is for " + shareClassId
                            + ". Send the configuration to its own share class.", "shareClassId");
        }
        NumberingChange change = new NumberingChange(body.originalStart(), body.replacementStart(), body.mode());
        return NumberingConfig.from(numbering.configure(shareClassId, change));
    }
}
