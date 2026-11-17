package com.iqra.zakat.controller;

import com.iqra.zakat.dto.MasterDtos;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.service.MasterServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public class MasterControllers {

    // =========================================================================
    // 1. Classification
    // =========================================================================
    @RestController
    @RequestMapping("/api/v1/classifications")
    @RequiredArgsConstructor
    @Tag(name = "Masters - Classifications", description = "Classification management")
    public static class ClassificationController {
        private final MasterServices.ClassificationService service;

        @GetMapping
        @Operation(summary = "List classifications")
        public ResponseEntity<PagedResponse<MasterDtos.ClassificationResponse>> list(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(required = false) String sort,
                @RequestParam(required = false) String q) {
            return ResponseEntity.ok(service.list(page, size, sort, q));
        }

        @GetMapping("/active")
        @Operation(summary = "Get active classifications")
        public ResponseEntity<List<MasterDtos.ClassificationResponse>> getActive() {
            return ResponseEntity.ok(service.getActive());
        }

        @GetMapping("/{id}")
        public ResponseEntity<MasterDtos.ClassificationResponse> getById(@PathVariable Long id) {
            return ResponseEntity.ok(service.getById(id));
        }

        @PostMapping
        public ResponseEntity<MasterDtos.ClassificationResponse> create(@Valid @RequestBody MasterDtos.ClassificationRequest req) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        }

        @PutMapping("/{id}")
        public ResponseEntity<MasterDtos.ClassificationResponse> update(@PathVariable Long id, @Valid @RequestBody MasterDtos.ClassificationRequest req) {
            return ResponseEntity.ok(service.update(id, req));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
            service.delete(id);
            return ResponseEntity.noContent().build();
        }
    }

    // =========================================================================
    // 2. Beneficiary Category
    // =========================================================================
    @RestController
    @RequestMapping("/api/v1/beneficiary-categories")
    @RequiredArgsConstructor
    @Tag(name = "Masters - Beneficiary Categories", description = "Category management")
    public static class BeneficiaryCategoryController {
        private final MasterServices.BeneficiaryCategoryService service;

        @GetMapping
        public ResponseEntity<PagedResponse<MasterDtos.CategoryResponse>> list(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(required = false) String sort,
                @RequestParam(required = false) String q) {
            return ResponseEntity.ok(service.list(page, size, sort, q));
        }

        @GetMapping("/active")
        public ResponseEntity<List<MasterDtos.CategoryResponse>> getActive() {
            return ResponseEntity.ok(service.getActive());
        }

        @GetMapping("/{id}")
        public ResponseEntity<MasterDtos.CategoryResponse> getById(@PathVariable Long id) {
            return ResponseEntity.ok(service.getById(id));
        }

        @PostMapping
        public ResponseEntity<MasterDtos.CategoryResponse> create(@Valid @RequestBody MasterDtos.CategoryRequest req) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        }

        @PutMapping("/{id}")
        public ResponseEntity<MasterDtos.CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody MasterDtos.CategoryRequest req) {
            return ResponseEntity.ok(service.update(id, req));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
            service.delete(id);
            return ResponseEntity.noContent().build();
        }
    }

    // =========================================================================
    // 3. Source Account
    // =========================================================================
    @RestController
    @RequestMapping("/api/v1/source-accounts")
    @RequiredArgsConstructor
    @Tag(name = "Masters - Source Accounts", description = "Source account management")
    public static class SourceAccountController {
        private final MasterServices.SourceAccountService service;

        @GetMapping
        public ResponseEntity<PagedResponse<MasterDtos.SourceAccountResponse>> list(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(required = false) String sort,
                @RequestParam(required = false) String q) {
            return ResponseEntity.ok(service.list(page, size, sort, q));
        }

        @GetMapping("/active")
        public ResponseEntity<List<MasterDtos.SourceAccountResponse>> getActive() {
            return ResponseEntity.ok(service.getActive());
        }

        @GetMapping("/{id}")
        public ResponseEntity<MasterDtos.SourceAccountResponse> getById(@PathVariable Long id) {
            return ResponseEntity.ok(service.getById(id));
        }

        @PostMapping
        public ResponseEntity<MasterDtos.SourceAccountResponse> create(@Valid @RequestBody MasterDtos.SourceAccountRequest req) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        }

        @PutMapping("/{id}")
        public ResponseEntity<MasterDtos.SourceAccountResponse> update(@PathVariable Long id, @Valid @RequestBody MasterDtos.SourceAccountRequest req) {
            return ResponseEntity.ok(service.update(id, req));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
            service.delete(id);
            return ResponseEntity.noContent().build();
        }
    }

    // =========================================================================
    // 4. Agent
    // =========================================================================
    @RestController
    @RequestMapping("/api/v1/agents")
    @RequiredArgsConstructor
    @Tag(name = "Masters - Agents", description = "Agent management")
    public static class AgentController {
        private final MasterServices.AgentService service;

        @GetMapping
        public ResponseEntity<PagedResponse<MasterDtos.AgentResponse>> list(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(required = false) String sort,
                @RequestParam(required = false) String q) {
            return ResponseEntity.ok(service.list(page, size, sort, q));
        }

        @GetMapping("/active")
        public ResponseEntity<List<MasterDtos.AgentResponse>> getActive() {
            return ResponseEntity.ok(service.getActive());
        }

        @GetMapping("/{id}")
        public ResponseEntity<MasterDtos.AgentResponse> getById(@PathVariable Long id) {
            return ResponseEntity.ok(service.getById(id));
        }

        @PostMapping
        public ResponseEntity<MasterDtos.AgentResponse> create(@Valid @RequestBody MasterDtos.AgentRequest req) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        }

        @PutMapping("/{id}")
        public ResponseEntity<MasterDtos.AgentResponse> update(@PathVariable Long id, @Valid @RequestBody MasterDtos.AgentRequest req) {
            return ResponseEntity.ok(service.update(id, req));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
            service.delete(id);
            return ResponseEntity.noContent().build();
        }
    }

    // =========================================================================
    // 5. Employee
    // =========================================================================
    @RestController
    @RequestMapping("/api/v1/employees")
    @RequiredArgsConstructor
    @Tag(name = "Masters - Employees", description = "Employee management")
    public static class EmployeeController {
        private final MasterServices.EmployeeService service;

        @GetMapping
        public ResponseEntity<PagedResponse<MasterDtos.EmployeeResponse>> list(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(required = false) String sort,
                @RequestParam(required = false) String q) {
            return ResponseEntity.ok(service.list(page, size, sort, q));
        }

        @GetMapping("/active")
        public ResponseEntity<List<MasterDtos.EmployeeResponse>> getActive() {
            return ResponseEntity.ok(service.getActive());
        }

        @GetMapping("/{id}")
        public ResponseEntity<MasterDtos.EmployeeResponse> getById(@PathVariable Long id) {
            return ResponseEntity.ok(service.getById(id));
        }

        @PostMapping
        public ResponseEntity<MasterDtos.EmployeeResponse> create(@Valid @RequestBody MasterDtos.EmployeeRequest req) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        }

        @PutMapping("/{id}")
        public ResponseEntity<MasterDtos.EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody MasterDtos.EmployeeRequest req) {
            return ResponseEntity.ok(service.update(id, req));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
            service.delete(id);
            return ResponseEntity.noContent().build();
        }
    }

    // =========================================================================
    // 6. School / Institute
    // =========================================================================
    @RestController
    @RequestMapping("/api/v1/schools")
    @RequiredArgsConstructor
    @Tag(name = "Masters - Schools / Institutes", description = "School and Institute management")
    public static class SchoolController {
        private final MasterServices.SchoolService service;

        @GetMapping
        public ResponseEntity<PagedResponse<MasterDtos.SchoolResponse>> list(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "50") int size,
                @RequestParam(required = false) String sort,
                @RequestParam(required = false) String q) {
            return ResponseEntity.ok(service.list(page, size, sort, q));
        }

        @GetMapping("/active")
        public ResponseEntity<List<MasterDtos.SchoolResponse>> getActive() {
            return ResponseEntity.ok(service.getActive());
        }

        @GetMapping("/{id}")
        public ResponseEntity<MasterDtos.SchoolResponse> getById(@PathVariable Long id) {
            return ResponseEntity.ok(service.getById(id));
        }

        @PostMapping
        public ResponseEntity<MasterDtos.SchoolResponse> create(@Valid @RequestBody MasterDtos.SchoolRequest req) {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
        }

        @PutMapping("/{id}")
        public ResponseEntity<MasterDtos.SchoolResponse> update(@PathVariable Long id, @Valid @RequestBody MasterDtos.SchoolRequest req) {
            return ResponseEntity.ok(service.update(id, req));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
            service.delete(id);
            return ResponseEntity.noContent().build();
        }
    }
}
