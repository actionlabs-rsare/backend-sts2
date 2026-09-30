package com.ap.sts.company;

import com.ap.sts.company.CompanyDtos.CompanyInput;
import com.ap.sts.company.CompanyDtos.CompanyResponse;
import com.ap.sts.shared.auth.Permission;
import com.ap.sts.shared.auth.RequiresPermission;
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

/** Company maintenance API (MDM-001), matching companies.openapi.yaml. */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission(module = "company", action = Permission.list)
    public List<CompanyResponse> list(@RequestParam(required = false) String q) {
        return service.list(q).stream().map(CompanyResponse::from).toList();
    }

    @GetMapping("/{code}")
    @RequiresPermission(module = "company", action = Permission.view)
    public CompanyResponse get(@PathVariable String code) {
        return CompanyResponse.from(service.get(code));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission(module = "company", action = Permission.add)
    public CompanyResponse create(@Valid @RequestBody CompanyInput input) {
        return CompanyResponse.from(service.create(input));
    }

    @PutMapping("/{code}")
    @RequiresPermission(module = "company", action = Permission.change)
    public CompanyResponse update(@PathVariable String code, @Valid @RequestBody CompanyInput input) {
        return CompanyResponse.from(service.update(code, input));
    }
}
