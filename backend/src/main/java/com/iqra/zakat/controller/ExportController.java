package com.iqra.zakat.controller;

import com.iqra.zakat.dto.AllocationDto;
import com.iqra.zakat.dto.AssessmentDto;
import com.iqra.zakat.dto.BeneficiaryDto;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.service.AllocationService;
import com.iqra.zakat.service.AssessmentService;
import com.iqra.zakat.service.BeneficiaryService;
import com.iqra.zakat.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Exports", description = "Export reports to Excel and CSV")
public class ExportController {

    private final ExportService exportService;
    private final AllocationService allocationService;
    private final BeneficiaryService beneficiaryService;
    private final AssessmentService assessmentService;

    @GetMapping("/allocations.xlsx")
    @Operation(summary = "Export allocations to Excel (full filtered result set)")
    public ResponseEntity<byte[]> exportAllocations(
            @RequestParam(required = false) List<Integer> years,
            @RequestParam(required = false) Long classificationId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Long beneficiaryId,
            @RequestParam(required = false) Long sourceAccountId,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String q) {

        PagedResponse<AllocationDto.Response> paged = allocationService.list(
                0, 10000, "zakatYear,desc", years, classificationId, category,
                status, approvalStatus, beneficiaryId, sourceAccountId, schoolId, paymentMethod, q);

        byte[] bytes = exportService.exportAllocationsToExcel(paged.getContent());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"allocations.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/beneficiaries.xlsx")
    @Operation(summary = "Export beneficiaries to Excel")
    public ResponseEntity<byte[]> exportBeneficiaries(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(required = false) String isActive,
            @RequestParam(required = false) String q) {

        PagedResponse<BeneficiaryDto.Response> paged = beneficiaryService.list(
                0, 10000, "name,asc", category, year, verificationStatus, isActive, q);

        byte[] bytes = exportService.exportBeneficiariesToExcel(paged.getContent());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"beneficiaries.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/assessments.xlsx")
    @Operation(summary = "Export assessments to Excel")
    public ResponseEntity<byte[]> exportAssessments() {
        PagedResponse<AssessmentDto.Response> paged = assessmentService.getAssessments(0, 100, "zakatYear,desc", null);
        byte[] bytes = exportService.exportAssessmentsToExcel(paged.getContent());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"zakat-assessments.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
