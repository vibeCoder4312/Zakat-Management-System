package com.iqra.zakat.controller;

import com.iqra.zakat.dto.BeneficiaryDto;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries", description = "Beneficiary management and verification")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @GetMapping
    @Operation(summary = "List beneficiaries with filters and search")
    public ResponseEntity<PagedResponse<BeneficiaryDto.Response>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(required = false) String isActive,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(beneficiaryService.list(page, size, sort, category, year, verificationStatus, isActive, q));
    }

    @GetMapping("/typeahead")
    @Operation(summary = "Search verified and active beneficiaries for allocation dropdown")
    public ResponseEntity<List<BeneficiaryDto.Response>> getTypeahead(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(beneficiaryService.getTypeahead(q, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get beneficiary by ID")
    public ResponseEntity<BeneficiaryDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(beneficiaryService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create beneficiary")
    public ResponseEntity<BeneficiaryDto.Response> create(@Valid @RequestBody BeneficiaryDto.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(beneficiaryService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update beneficiary")
    public ResponseEntity<BeneficiaryDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody BeneficiaryDto.Request req) {
        return ResponseEntity.ok(beneficiaryService.update(id, req));
    }

    @PatchMapping("/{id}/verification")
    @Operation(summary = "Update beneficiary verification status")
    public ResponseEntity<BeneficiaryDto.Response> updateVerification(
            @PathVariable Long id,
            @Valid @RequestBody BeneficiaryDto.VerificationRequest req) {
        return ResponseEntity.ok(beneficiaryService.updateVerification(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete beneficiary (fails if allocations exist)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        beneficiaryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
