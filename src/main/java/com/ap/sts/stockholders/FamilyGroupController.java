package com.ap.sts.stockholders;

import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.RequiresPermission;
import com.ap.sts.stockholders.StockholderDtos.FamilyGroupInput;
import com.ap.sts.stockholders.StockholderDtos.FamilyGroupResponse;
import com.ap.sts.stockholders.StockholderDtos.StockholderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Family groups and the grouping inquiry (MDM-002-FG, OI-15), matching
 * {@code contracts/stockholders.openapi.yaml}. Guarded by the {@code stockholder} module
 * because a group is a view over stockholder personal data.
 */
@RestController
@RequestMapping("/api/family-groups")
public class FamilyGroupController {

    private final FamilyGroupService service;

    public FamilyGroupController(FamilyGroupService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission(module = "stockholder", action = Permission.list)
    public List<FamilyGroupResponse> list() {
        return service.list().stream().map(FamilyGroupResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(module = "stockholder", action = Permission.add)
    public FamilyGroupResponse create(@Valid @RequestBody FamilyGroupInput input) {
        return FamilyGroupResponse.from(service.create(input.name()));
    }

    @GetMapping("/{id}/stockholders")
    @RequiresPermission(module = "stockholder", action = Permission.view)
    public List<StockholderResponse> members(@PathVariable String id) {
        return service.members(id);
    }
}
