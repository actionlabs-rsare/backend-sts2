package com.ap.sts.shares;

import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.RequiresPermission;
import com.ap.sts.shares.ShareClassDtos.ShareClassInput;
import com.ap.sts.shares.ShareClassDtos.ShareClassResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Company share maintenance API (MDM-003), matching {@code contracts/shares.openapi.yaml}.
 *
 * <p>The update path is additive to the frozen contract (change request CR-8): the contract has
 * GET and POST only, but a share class whose counts can never be corrected would force AP to
 * create a duplicate class, which would break the one-sequence-per-class rule.
 */
@RestController
@RequestMapping("/api/companies/{companyCode}/share-classes")
public class ShareClassController {

    private final ShareClassService service;

    public ShareClassController(ShareClassService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission(module = "share", action = Permission.list)
    public List<ShareClassResponse> list(@PathVariable String companyCode) {
        return service.list(companyCode).stream().map(service::response).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(module = "share", action = Permission.add)
    public ShareClassResponse create(@PathVariable String companyCode,
                                     @Valid @RequestBody ShareClassInput input) {
        return service.response(service.create(companyCode, input));
    }

    @PutMapping("/{id}")
    @RequiresPermission(module = "share", action = Permission.change)
    public ShareClassResponse update(@PathVariable String companyCode,
                                     @PathVariable String id,
                                     @Valid @RequestBody ShareClassInput input) {
        return service.response(service.update(id, input));
    }
}
