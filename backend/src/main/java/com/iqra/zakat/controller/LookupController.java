package com.iqra.zakat.controller;

import com.iqra.zakat.dto.BeneficiaryDto;
import com.iqra.zakat.dto.MasterDtos;
import com.iqra.zakat.repository.*;
import com.iqra.zakat.service.BeneficiaryService;
import com.iqra.zakat.service.MasterServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lookups")
@RequiredArgsConstructor
@Tag(name = "Lookups", description = "Dropdown lookups and reference data")
public class LookupController {

    private final AssessmentRepository assessmentRepository;
    private final MasterServices.ClassificationService classificationService;
    private final MasterServices.BeneficiaryCategoryService categoryService;
    private final MasterServices.SourceAccountService sourceAccountService;
    private final MasterServices.SchoolService schoolService;
    private final MasterServices.AgentService agentService;
    private final MasterServices.EmployeeService employeeService;
    private final BeneficiaryService beneficiaryService;

    @GetMapping("/years")
    @Operation(summary = "Get all distinct Zakat years")
    public ResponseEntity<List<Integer>> getYears() {
        return ResponseEntity.ok(assessmentRepository.findAllDistinctYears());
    }

    @GetMapping("/classifications")
    @Operation(summary = "Get active classifications")
    public ResponseEntity<List<MasterDtos.ClassificationResponse>> getClassifications() {
        return ResponseEntity.ok(classificationService.getActive());
    }

    @GetMapping("/categories")
    @Operation(summary = "Get active beneficiary categories")
    public ResponseEntity<List<MasterDtos.CategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getActive());
    }

    @GetMapping("/source-accounts")
    @Operation(summary = "Get active source accounts")
    public ResponseEntity<List<MasterDtos.SourceAccountResponse>> getSourceAccounts() {
        return ResponseEntity.ok(sourceAccountService.getActive());
    }

    @GetMapping("/schools")
    @Operation(summary = "Get active institutes / schools")
    public ResponseEntity<List<MasterDtos.SchoolResponse>> getSchools() {
        return ResponseEntity.ok(schoolService.getActive());
    }

    @GetMapping("/agents")
    @Operation(summary = "Get active agents")
    public ResponseEntity<List<MasterDtos.AgentResponse>> getAgents() {
        return ResponseEntity.ok(agentService.getActive());
    }

    @GetMapping("/employees")
    @Operation(summary = "Get active employees")
    public ResponseEntity<List<MasterDtos.EmployeeResponse>> getEmployees() {
        return ResponseEntity.ok(employeeService.getActive());
    }

    @GetMapping("/beneficiaries-verified")
    @Operation(summary = "Get verified active beneficiaries typeahead")
    public ResponseEntity<List<BeneficiaryDto.Response>> getVerifiedBeneficiaries(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(beneficiaryService.getTypeahead(q, limit));
    }
}
