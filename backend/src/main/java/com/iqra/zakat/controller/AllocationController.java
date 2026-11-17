package com.iqra.zakat.controller;

import com.iqra.zakat.dto.AllocationDto;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.service.AllocationService;
import com.iqra.zakat.service.AllocationService.ReceiptDownload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/allocations", "/api/allocations"})
@RequiredArgsConstructor
@Tag(name = "Allocations", description = "Zakat and Sadaqah allocation and payment operations")
public class AllocationController {

    private final AllocationService allocationService;

    @GetMapping
    @Operation(summary = "List allocations with filtering, sorting, and pagination")
    public ResponseEntity<PagedResponse<AllocationDto.Response>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
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

        return ResponseEntity.ok(allocationService.list(
                page, size, sort, years, classificationId, category,
                status, approvalStatus, beneficiaryId, sourceAccountId, schoolId, paymentMethod, q));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get aggregate summary (counts, totals) for allocations matching filters")
    public ResponseEntity<AllocationDto.SummaryResponse> getSummary(
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

        return ResponseEntity.ok(allocationService.getSummary(
                years, classificationId, category, status, approvalStatus,
                beneficiaryId, sourceAccountId, schoolId, paymentMethod, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get allocation details by ID")
    public ResponseEntity<AllocationDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(allocationService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new allocation")
    public ResponseEntity<AllocationDto.Response> create(@Valid @RequestBody AllocationDto.Request req) {
        return ResponseEntity.status(201).body(allocationService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update allocation")
    public ResponseEntity<AllocationDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody AllocationDto.Request req) {
        return ResponseEntity.ok(allocationService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete allocation")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        allocationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload receipt file for an allocation")
    public ResponseEntity<Void> uploadReceipt(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        allocationService.uploadReceipt(id, file.getBytes(), file.getOriginalFilename(), file.getContentType());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Download receipt file for an allocation")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        ReceiptDownload receipt = allocationService.downloadReceipt(id);
        if (receipt == null || receipt.bytes() == null || receipt.bytes().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String contentDisposition = ContentDisposition.attachment()
                .filename(receipt.filename(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(receipt.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(receipt.bytes());
    }
}
