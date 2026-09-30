package com.ap.sts.stockholders;

import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.RequiresPermission;
import com.ap.sts.stockholders.StockholderDtos.StockholderInput;
import com.ap.sts.stockholders.StockholderDtos.StockholderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Stockholder maintenance API (MDM-002), matching {@code contracts/stockholders.openapi.yaml}.
 *
 * <p>Every handler declares a permission, so the deny-by-default interceptor (SL-005 / SECURITY-08)
 * covers the whole surface. There is no delete mapping and there must not be one (OI-19):
 * retiring a stockholder is {@code PUT} with {@code active: false}.
 */
@RestController
@RequestMapping("/api/stockholders")
public class StockholderController {

    private final StockholderService service;

    public StockholderController(StockholderService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission(module = "stockholder", action = Permission.list)
    public List<StockholderResponse> list(@RequestParam(required = false) String q) {
        return service.list(q).stream().map(StockholderResponse::from).toList();
    }

    @GetMapping("/{code}")
    @RequiresPermission(module = "stockholder", action = Permission.view)
    public StockholderResponse get(@PathVariable String code) {
        return StockholderResponse.from(service.get(code));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(module = "stockholder", action = Permission.add)
    public StockholderResponse create(@Valid @RequestBody StockholderInput input) {
        return StockholderResponse.from(service.create(input));
    }

    @PutMapping("/{code}")
    @RequiresPermission(module = "stockholder", action = Permission.change)
    public StockholderResponse update(@PathVariable String code, @Valid @RequestBody StockholderInput input) {
        return StockholderResponse.from(service.update(code, input));
    }
}
