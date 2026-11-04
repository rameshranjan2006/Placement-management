package com.placement.controller;

import com.placement.model.Company;
import com.placement.security.AccessGuard;
import com.placement.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final AccessGuard guard;

    @GetMapping("/me")
    public ResponseEntity<Company> myProfile() {
        return ResponseEntity.ok(guard.company());
    }

    @PutMapping("/me")
    public ResponseEntity<Company> updateMyProfile(@Valid @RequestBody Company updated) {
        return ResponseEntity.ok(companyService.updateProfile(guard.company(), updated));
    }
}
