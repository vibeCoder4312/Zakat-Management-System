package com.iqra.zakat.service;

import com.iqra.zakat.dto.MasterDtos;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.entity.*;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.exception.ResourceNotFoundException;
import com.iqra.zakat.mapper.EntityMappers;
import com.iqra.zakat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public class MasterServices {

    // =========================================================================
    // 1. ClassificationService
    // =========================================================================
    @Service
    @RequiredArgsConstructor
    public static class ClassificationService {
        private final ClassificationRepository repository;
        private final AllocationRepository allocationRepository;
        private final PlanningRepository planningRepository;
        private final EntityMappers mapper;
        private final AuthService authService;

        @Transactional(readOnly = true)
        public PagedResponse<MasterDtos.ClassificationResponse> list(int page, int size, String sort, String q) {
            Sort sortObj = Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by(Sort.Direction.ASC, "name"));
            Pageable pageable = PageRequest.of(page, size, sortObj);
            Page<Classification> result = repository.searchClassifications(q, pageable);
            return PagedResponse.<MasterDtos.ClassificationResponse>builder()
                    .content(result.getContent().stream().map(mapper::toClassificationResponse).toList())
                    .pageNumber(result.getNumber())
                    .pageSize(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .last(result.isLast())
                    .build();
        }

        @Transactional(readOnly = true)
        public List<MasterDtos.ClassificationResponse> getActive() {
            return repository.findByIsActiveOrderByDisplayOrderAscNameAsc("Y").stream()
                    .map(mapper::toClassificationResponse)
                    .toList();
        }

        @Transactional(readOnly = true)
        public MasterDtos.ClassificationResponse getById(Long id) {
            return mapper.toClassificationResponse(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Classification", id)));
        }

        @Transactional
        public MasterDtos.ClassificationResponse create(MasterDtos.ClassificationRequest req) {
            if (repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Classification with name '" + req.getName() + "' already exists.");
            }
            Classification entity = mapper.toClassificationEntity(req);
            String u = authService.getCurrentUsername();
            entity.setCreatedBy(u);
            entity.setCreatedOn(OffsetDateTime.now());
            entity.setUpdatedBy(u);
            entity.setUpdatedOn(OffsetDateTime.now());
            return mapper.toClassificationResponse(repository.save(entity));
        }

        @Transactional
        public MasterDtos.ClassificationResponse update(Long id, MasterDtos.ClassificationRequest req) {
            Classification existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Classification", id));
            if (!existing.getName().equalsIgnoreCase(req.getName()) && repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Classification with name '" + req.getName() + "' already exists.");
            }
            String oldName = existing.getName();

            existing.setCode(req.getCode());
            existing.setName(req.getName());
            existing.setFundType(req.getFundType());
            existing.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : existing.getDisplayOrder());
            existing.setIsActive(req.getIsActive() != null ? req.getIsActive() : existing.getIsActive());
            existing.setUpdatedBy(authService.getCurrentUsername());
            existing.setUpdatedOn(OffsetDateTime.now());

            Classification saved = repository.save(existing);
            if (oldName != null && !oldName.equalsIgnoreCase(saved.getName())) {
                allocationRepository.updateClassificationName(id, saved.getName());
            }
            return mapper.toClassificationResponse(saved);
        }

        @Transactional
        public void delete(Long id) {
            Classification existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Classification", id));
            if (allocationRepository.existsByClassificationId(id) || planningRepository.existsByClassificationId(id)) {
                throw new BusinessRuleException("Cannot delete classification because it is referenced by existing allocations or plans. Deactivate it instead.");
            }
            repository.delete(existing);
        }
    }

    // =========================================================================
    // 2. BeneficiaryCategoryService
    // =========================================================================
    @Service
    @RequiredArgsConstructor
    public static class BeneficiaryCategoryService {
        private final BeneficiaryCategoryRepository repository;
        private final BeneficiaryRepository beneficiaryRepository;
        private final PlanningRepository planningRepository;
        private final AllocationRepository allocationRepository;
        private final EntityMappers mapper;
        private final AuthService authService;

        @Transactional(readOnly = true)
        public PagedResponse<MasterDtos.CategoryResponse> list(int page, int size, String sort, String q) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            Page<BeneficiaryCategory> result = repository.searchCategories(q, pageable);

            java.util.Map<String, Long> counts = beneficiaryCounts();
            List<MasterDtos.CategoryResponse> list = result.getContent().stream()
                    .map(cat -> mapper.toCategoryResponse(cat, counts.getOrDefault(cat.getName().toLowerCase(), 0L)))
                    .toList();

            return PagedResponse.<MasterDtos.CategoryResponse>builder()
                    .content(list)
                    .pageNumber(result.getNumber())
                    .pageSize(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .last(result.isLast())
                    .build();
        }

        /** Beneficiary count per category name (case-insensitive), as shown on the APEX master report. */
        private java.util.Map<String, Long> beneficiaryCounts() {
            java.util.Map<String, Long> m = new java.util.HashMap<>();
            for (Object[] row : beneficiaryRepository.countByCategory()) {
                if (row[0] != null) m.merge(row[0].toString().toLowerCase(), ((Number) row[1]).longValue(), Long::sum);
            }
            return m;
        }

        @Transactional(readOnly = true)
        public List<MasterDtos.CategoryResponse> getActive() {
            java.util.Map<String, Long> counts = beneficiaryCounts();
            return repository.findByIsActiveOrderByNameAsc("Y").stream()
                    .map(c -> mapper.toCategoryResponse(c, counts.getOrDefault(c.getName().toLowerCase(), 0L)))
                    .toList();
        }

        @Transactional(readOnly = true)
        public MasterDtos.CategoryResponse getById(Long id) {
            BeneficiaryCategory c = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Beneficiary Category", id));
            return mapper.toCategoryResponse(c, beneficiaryCounts().getOrDefault(c.getName().toLowerCase(), 0L));
        }

        @Transactional
        public MasterDtos.CategoryResponse create(MasterDtos.CategoryRequest req) {
            if (repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Beneficiary category with name '" + req.getName() + "' already exists.");
            }
            BeneficiaryCategory entity = mapper.toCategoryEntity(req);
            String u = authService.getCurrentUsername();
            entity.setCreatedBy(u);
            entity.setCreatedOn(OffsetDateTime.now());
            entity.setUpdatedBy(u);
            entity.setUpdatedOn(OffsetDateTime.now());
            return mapper.toCategoryResponse(repository.save(entity), 0L);
        }

        @Transactional
        public MasterDtos.CategoryResponse update(Long id, MasterDtos.CategoryRequest req) {
            BeneficiaryCategory existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Beneficiary Category", id));
            if (!existing.getName().equalsIgnoreCase(req.getName()) && repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Beneficiary category with name '" + req.getName() + "' already exists.");
            }
            String oldName = existing.getName();

            existing.setName(req.getName());
            existing.setIsActive(req.getIsActive() != null ? req.getIsActive() : existing.getIsActive());
            existing.setUpdatedBy(authService.getCurrentUsername());
            existing.setUpdatedOn(OffsetDateTime.now());

            BeneficiaryCategory saved = repository.save(existing);
            if (oldName != null && !oldName.equalsIgnoreCase(saved.getName())) {
                beneficiaryRepository.updateCategoryName(oldName, saved.getName());
                planningRepository.updateCategoryName(oldName, saved.getName());
                allocationRepository.updateCategoryName(oldName, saved.getName());
            }
            return mapper.toCategoryResponse(saved, 0L);
        }

        @Transactional
        public void delete(Long id) {
            BeneficiaryCategory existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Beneficiary Category", id));
            if (beneficiaryCounts().getOrDefault(existing.getName().toLowerCase(), 0L) > 0 ||
                    planningRepository.existsByCategoryIgnoreCase(existing.getName())) {
                throw new BusinessRuleException("Cannot delete category because beneficiaries or plans are assigned to it. Deactivate instead.");
            }
            repository.delete(existing);
        }
    }

    // =========================================================================
    // 3. SourceAccountService
    // =========================================================================
    @Service
    @RequiredArgsConstructor
    public static class SourceAccountService {
        private final SourceAccountRepository repository;
        private final AllocationRepository allocationRepository;
        private final EntityMappers mapper;
        private final AuthService authService;

        @Transactional(readOnly = true)
        public PagedResponse<MasterDtos.SourceAccountResponse> list(int page, int size, String sort, String q) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            Page<SourceAccount> result = repository.searchSourceAccounts(q, pageable);
            return PagedResponse.<MasterDtos.SourceAccountResponse>builder()
                    .content(result.getContent().stream().map(mapper::toSourceAccountResponse).toList())
                    .pageNumber(result.getNumber())
                    .pageSize(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .last(result.isLast())
                    .build();
        }

        @Transactional(readOnly = true)
        public List<MasterDtos.SourceAccountResponse> getActive() {
            return repository.findByIsActiveOrderByNameAsc("Y").stream()
                    .map(mapper::toSourceAccountResponse)
                    .toList();
        }

        @Transactional(readOnly = true)
        public MasterDtos.SourceAccountResponse getById(Long id) {
            return mapper.toSourceAccountResponse(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Source Account", id)));
        }

        @Transactional
        public MasterDtos.SourceAccountResponse create(MasterDtos.SourceAccountRequest req) {
            if (repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Source Account with name '" + req.getName() + "' already exists.");
            }
            SourceAccount entity = mapper.toSourceAccountEntity(req);
            String u = authService.getCurrentUsername();
            entity.setCreatedBy(u);
            entity.setCreatedOn(OffsetDateTime.now());
            entity.setUpdatedBy(u);
            entity.setUpdatedOn(OffsetDateTime.now());
            return mapper.toSourceAccountResponse(repository.save(entity));
        }

        @Transactional
        public MasterDtos.SourceAccountResponse update(Long id, MasterDtos.SourceAccountRequest req) {
            SourceAccount existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Source Account", id));
            if (!existing.getName().equalsIgnoreCase(req.getName()) && repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Source Account with name '" + req.getName() + "' already exists.");
            }
            String oldName = existing.getName();

            existing.setName(req.getName());
            existing.setRegion(req.getRegion());
            existing.setIsActive(req.getIsActive() != null ? req.getIsActive() : existing.getIsActive());
            existing.setUpdatedBy(authService.getCurrentUsername());
            existing.setUpdatedOn(OffsetDateTime.now());

            SourceAccount saved = repository.save(existing);
            if (oldName != null && !oldName.equalsIgnoreCase(saved.getName())) {
                allocationRepository.updatePaidSourceAccountName(id, saved.getName());
            }
            return mapper.toSourceAccountResponse(saved);
        }

        @Transactional
        public void delete(Long id) {
            SourceAccount existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Source Account", id));
            if (allocationRepository.existsByPaidSourceAccountId(id) || allocationRepository.existsByPlannedSourceAccountId(id)) {
                throw new BusinessRuleException("Cannot delete source account because it is referenced in transactions. Deactivate it instead.");
            }
            repository.delete(existing);
        }
    }

    // =========================================================================
    // 4. AgentService
    // =========================================================================
    @Service
    @RequiredArgsConstructor
    public static class AgentService {
        private final AgentRepository repository;
        private final AllocationRepository allocationRepository;
        private final EntityMappers mapper;
        private final AuthService authService;

        @Transactional(readOnly = true)
        public PagedResponse<MasterDtos.AgentResponse> list(int page, int size, String sort, String q) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            Page<Agent> result = repository.searchAgents(q, pageable);
            return PagedResponse.<MasterDtos.AgentResponse>builder()
                    .content(result.getContent().stream().map(mapper::toAgentResponse).toList())
                    .pageNumber(result.getNumber())
                    .pageSize(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .last(result.isLast())
                    .build();
        }

        @Transactional(readOnly = true)
        public List<MasterDtos.AgentResponse> getActive() {
            return repository.findByIsActiveOrderByNameAsc("Y").stream()
                    .map(mapper::toAgentResponse)
                    .toList();
        }

        @Transactional(readOnly = true)
        public MasterDtos.AgentResponse getById(Long id) {
            return mapper.toAgentResponse(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent", id)));
        }

        @Transactional
        public MasterDtos.AgentResponse create(MasterDtos.AgentRequest req) {
            if (repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Agent with name '" + req.getName() + "' already exists.");
            }
            Agent entity = mapper.toAgentEntity(req);
            String u = authService.getCurrentUsername();
            entity.setCreatedBy(u);
            entity.setCreatedOn(OffsetDateTime.now());
            entity.setUpdatedBy(u);
            entity.setUpdatedOn(OffsetDateTime.now());
            return mapper.toAgentResponse(repository.save(entity));
        }

        @Transactional
        public MasterDtos.AgentResponse update(Long id, MasterDtos.AgentRequest req) {
            Agent existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
            if (!existing.getName().equalsIgnoreCase(req.getName()) && repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Agent with name '" + req.getName() + "' already exists.");
            }
            String oldName = existing.getName();

            existing.setName(req.getName());
            existing.setAgentType(req.getAgentType());
            existing.setContactNumber(req.getContactNumber());
            existing.setIsActive(req.getIsActive() != null ? req.getIsActive() : existing.getIsActive());
            existing.setUpdatedBy(authService.getCurrentUsername());
            existing.setUpdatedOn(OffsetDateTime.now());

            Agent saved = repository.save(existing);
            if (oldName != null && !oldName.equalsIgnoreCase(saved.getName())) {
                allocationRepository.updatePaidViaAgentName(id, saved.getName());
            }
            return mapper.toAgentResponse(saved);
        }

        @Transactional
        public void delete(Long id) {
            Agent existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent", id));
            if (allocationRepository.existsByPaidViaAgentId(id)) {
                throw new BusinessRuleException("Cannot delete agent because allocations reference this agent. Deactivate instead.");
            }
            repository.delete(existing);
        }
    }

    // =========================================================================
    // 5. EmployeeService
    // =========================================================================
    @Service
    @RequiredArgsConstructor
    public static class EmployeeService {
        private final EmployeeRepository repository;
        private final AllocationRepository allocationRepository;
        private final EntityMappers mapper;
        private final AuthService authService;

        @Transactional(readOnly = true)
        public PagedResponse<MasterDtos.EmployeeResponse> list(int page, int size, String sort, String q) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            Page<Employee> result = repository.searchEmployees(q, pageable);
            return PagedResponse.<MasterDtos.EmployeeResponse>builder()
                    .content(result.getContent().stream().map(mapper::toEmployeeResponse).toList())
                    .pageNumber(result.getNumber())
                    .pageSize(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .last(result.isLast())
                    .build();
        }

        @Transactional(readOnly = true)
        public List<MasterDtos.EmployeeResponse> getActive() {
            return repository.findByIsActiveOrderByNameAsc("Y").stream()
                    .map(mapper::toEmployeeResponse)
                    .toList();
        }

        @Transactional(readOnly = true)
        public MasterDtos.EmployeeResponse getById(Long id) {
            return mapper.toEmployeeResponse(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", id)));
        }

        @Transactional
        public MasterDtos.EmployeeResponse create(MasterDtos.EmployeeRequest req) {
            if (repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Employee with name '" + req.getName() + "' already exists.");
            }
            Employee entity = mapper.toEmployeeEntity(req);
            String u = authService.getCurrentUsername();
            entity.setCreatedBy(u);
            entity.setCreatedOn(OffsetDateTime.now());
            entity.setUpdatedBy(u);
            entity.setUpdatedOn(OffsetDateTime.now());
            return mapper.toEmployeeResponse(repository.save(entity));
        }

        @Transactional
        public MasterDtos.EmployeeResponse update(Long id, MasterDtos.EmployeeRequest req) {
            Employee existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
            if (!existing.getName().equalsIgnoreCase(req.getName()) && repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Employee with name '" + req.getName() + "' already exists.");
            }
            String oldName = existing.getName();

            existing.setName(req.getName());
            existing.setContactNumber(req.getContactNumber());
            existing.setRemarks(req.getRemarks());
            existing.setIsActive(req.getIsActive() != null ? req.getIsActive() : existing.getIsActive());
            existing.setUpdatedBy(authService.getCurrentUsername());
            existing.setUpdatedOn(OffsetDateTime.now());

            Employee saved = repository.save(existing);
            if (oldName != null && !oldName.equalsIgnoreCase(saved.getName())) {
                allocationRepository.updatePaidViaEmployeeName(oldName, saved.getName());
            }
            return mapper.toEmployeeResponse(saved);
        }

        @Transactional
        public void delete(Long id) {
            Employee existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
            if (allocationRepository.existsByPaidViaEmployeeIgnoreCase(existing.getName())) {
                throw new BusinessRuleException("Cannot delete employee because they are referenced in allocations. Deactivate instead.");
            }
            repository.delete(existing);
        }
    }

    // =========================================================================
    // 6. SchoolService (Institute)
    // =========================================================================
    @Service
    @RequiredArgsConstructor
    public static class SchoolService {
        private final SchoolRepository repository;
        private final AllocationRepository allocationRepository;
        private final EntityMappers mapper;
        private final AuthService authService;

        /** Allocation rows per institute: matches the "Beneficiary Count" column of the APEX Institute report. */
        private java.util.Map<Long, Long> allocationCounts() {
            java.util.Map<Long, Long> m = new java.util.HashMap<>();
            for (Object[] row : allocationRepository.countBySchool()) {
                m.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
            return m;
        }

        @Transactional(readOnly = true)
        public PagedResponse<MasterDtos.SchoolResponse> list(int page, int size, String sort, String q) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            Page<School> result = repository.searchSchools(q, pageable);
            java.util.Map<Long, Long> counts = allocationCounts();
            return PagedResponse.<MasterDtos.SchoolResponse>builder()
                    .content(result.getContent().stream().map(s -> mapper.toSchoolResponse(s, counts.getOrDefault(s.getSchoolId(), 0L))).toList())
                    .pageNumber(result.getNumber())
                    .pageSize(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .last(result.isLast())
                    .build();
        }

        @Transactional(readOnly = true)
        public List<MasterDtos.SchoolResponse> getActive() {
            java.util.Map<Long, Long> counts = allocationCounts();
            return repository.findByIsActiveOrderByNameAsc("Y").stream()
                    .map(s -> mapper.toSchoolResponse(s, counts.getOrDefault(s.getSchoolId(), 0L)))
                    .toList();
        }

        @Transactional(readOnly = true)
        public MasterDtos.SchoolResponse getById(Long id) {
            return mapper.toSchoolResponse(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("School", id)), allocationCounts().getOrDefault(id, 0L));
        }

        @Transactional
        public MasterDtos.SchoolResponse create(MasterDtos.SchoolRequest req) {
            if (repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Institute with name '" + req.getName() + "' already exists.");
            }
            School entity = mapper.toSchoolEntity(req);
            String u = authService.getCurrentUsername();
            entity.setCreatedBy(u);
            entity.setCreatedOn(OffsetDateTime.now());
            entity.setUpdatedBy(u);
            entity.setUpdatedOn(OffsetDateTime.now());
            return mapper.toSchoolResponse(repository.save(entity), 0L);
        }

        @Transactional
        public MasterDtos.SchoolResponse update(Long id, MasterDtos.SchoolRequest req) {
            School existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("School", id));
            if (!existing.getName().equalsIgnoreCase(req.getName()) && repository.existsByNameIgnoreCase(req.getName())) {
                throw new BusinessRuleException("Institute with name '" + req.getName() + "' already exists.");
            }
            String oldName = existing.getName();

            existing.setName(req.getName());
            existing.setLocation(req.getLocation());
            existing.setIsActive(req.getIsActive() != null ? req.getIsActive() : existing.getIsActive());
            existing.setUpdatedBy(authService.getCurrentUsername());
            existing.setUpdatedOn(OffsetDateTime.now());

            School saved = repository.save(existing);
            if (oldName != null && !oldName.equalsIgnoreCase(saved.getName())) {
                allocationRepository.updateSchoolName(id, saved.getName());
            }
            return mapper.toSchoolResponse(saved, 0L);
        }

        @Transactional
        public void delete(Long id) {
            School existing = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("School", id));
            if (allocationRepository.existsBySchoolId(id)) {
                throw new BusinessRuleException("Cannot delete institute because it is referenced in allocations. Deactivate instead.");
            }
            repository.delete(existing);
        }
    }
}
