package com.iqra.zakat.controller;

import com.iqra.zakat.dto.AssessmentDto;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
@Tag(name = "Assessments", description = "Yearly Zakat Assessment management")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping
    @Operation(summary = "List assessments with pagination and search")
    public ResponseEntity<PagedResponse<AssessmentDto.Response>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(assessmentService.getAssessments(page, size, sort, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assessment by ID")
    public ResponseEntity<AssessmentDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssessmentById(id));
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get assessment by Year")
    public ResponseEntity<AssessmentDto.Response> getByYear(@PathVariable Integer year) {
        return ResponseEntity.ok(assessmentService.getAssessmentByYear(year));
    }

    @PostMapping
    @Operation(summary = "Create assessment (ADMIN only)")
    public ResponseEntity<AssessmentDto.Response> create(@Valid @RequestBody AssessmentDto.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.createAssessment(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update assessment (ADMIN only)")
    public ResponseEntity<AssessmentDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody AssessmentDto.Request req) {
        return ResponseEntity.ok(assessmentService.updateAssessment(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete assessment (ADMIN only, fails if allocations exist)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return ResponseEntity.noContent().build();
    }
}
