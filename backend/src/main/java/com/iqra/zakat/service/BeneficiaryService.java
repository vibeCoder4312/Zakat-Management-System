package com.iqra.zakat.service;

import com.iqra.zakat.dto.BeneficiaryDto;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.entity.Beneficiary;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.exception.ResourceNotFoundException;
import com.iqra.zakat.mapper.EntityMappers;
import com.iqra.zakat.repository.AllocationRepository;
import com.iqra.zakat.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    private final BeneficiaryRepository repository;
    private final AllocationRepository allocationRepository;
    private final EntityMappers mapper;
    private final AuthService authService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "beneficiaryId", "beneficiaryCode", "name", "beneficiaryCategory", "zakatYear",
            "contactNumber", "address", "description", "referredBy", "verificationStatus",
            "verificationNote", "verifiedBy", "verifiedOn", "isActive", "createdOn", "updatedOn"
    );

    @Transactional(readOnly = true)
    public PagedResponse<BeneficiaryDto.Response> list(
            int page, int size, String sort,
            String category, Integer year, String verificationStatus, String isActive, String q) {

        Sort sortObj = Sort.by(Sort.Direction.DESC, "beneficiaryId");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            if (ALLOWED_SORT_FIELDS.contains(field)) {
                Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
                sortObj = Sort.by(dir, field);
            }
        }
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Beneficiary> result = repository.searchBeneficiaries(category, year, verificationStatus, isActive, q, pageable);

        return PagedResponse.<BeneficiaryDto.Response>builder()
                .content(result.getContent().stream().map(mapper::toBeneficiaryResponse).toList())
                .pageNumber(result.getNumber())
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public BeneficiaryDto.Response getById(Long id) {
        return mapper.toBeneficiaryResponse(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id)));
    }

    @Transactional(readOnly = true)
    public BeneficiaryDto.Response getByCode(String code) {
        return mapper.toBeneficiaryResponse(repository.findByBeneficiaryCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found for code: " + code)));
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryDto.Response> getTypeahead(String q, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository.findVerifiedActiveTypeahead(q, pageable).stream()
                .map(mapper::toBeneficiaryResponse)
                .toList();
    }

    @Transactional
    public BeneficiaryDto.Response create(BeneficiaryDto.Request req) {
        String code = repository.getNextBeneficiaryCode();
        Beneficiary entity = mapper.toBeneficiaryEntity(req);
        entity.setBeneficiaryCode(code);

        String username = authService.getCurrentUsername();

        // Rule 9: If created directly with VERIFIED, record the auditor and timestamp
        if ("VERIFIED".equalsIgnoreCase(entity.getVerificationStatus())) {
            entity.setVerifiedBy(username);
            entity.setVerifiedOn(OffsetDateTime.now());
        } else {
            entity.setVerifiedBy(null);
            entity.setVerifiedOn(null);
        }

        entity.setCreatedBy(username);
        entity.setCreatedOn(OffsetDateTime.now());
        entity.setUpdatedBy(username);
        entity.setUpdatedOn(OffsetDateTime.now());

        return mapper.toBeneficiaryResponse(repository.save(entity));
    }

    @Transactional
    public BeneficiaryDto.Response update(Long id, BeneficiaryDto.Request req) {
        Beneficiary existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));

        String oldName = existing.getName();
        String username = authService.getCurrentUsername();

        // Rule 9 & 10: Verification status transitions
        String newStatus = req.getVerificationStatus() != null ? req.getVerificationStatus() : existing.getVerificationStatus();
        if ("VERIFIED".equalsIgnoreCase(newStatus) && !"VERIFIED".equalsIgnoreCase(existing.getVerificationStatus())) {
            existing.setVerifiedBy(username);
            existing.setVerifiedOn(OffsetDateTime.now());
        } else if (!"VERIFIED".equalsIgnoreCase(newStatus)) {
            existing.setVerifiedBy(null);
            existing.setVerifiedOn(null);
        }

        existing.setName(req.getName());
        existing.setBeneficiaryCategory(req.getBeneficiaryCategory());
        existing.setZakatYear(req.getZakatYear());
        existing.setContactNumber(req.getContactNumber());
        existing.setAddress(req.getAddress());
        existing.setDescription(req.getDescription());
        existing.setReferredBy(req.getReferredBy());
        existing.setVerificationStatus(newStatus);
        existing.setVerificationNote(req.getVerificationNote());
        existing.setIsActive(req.getIsActive() != null ? req.getIsActive() : existing.getIsActive());

        existing.setUpdatedBy(username);
        existing.setUpdatedOn(OffsetDateTime.now());

        Beneficiary saved = repository.save(existing);
        if (oldName != null && !oldName.equalsIgnoreCase(saved.getName())) {
            allocationRepository.updateBeneficiaryName(id, saved.getName());
        }
        return mapper.toBeneficiaryResponse(saved);
    }

    @Transactional
    public BeneficiaryDto.Response updateVerification(Long id, BeneficiaryDto.VerificationRequest req) {
        Beneficiary existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));

        String username = authService.getCurrentUsername();

        existing.setVerificationStatus(req.getVerificationStatus());
        existing.setVerificationNote(req.getVerificationNote());

        if ("VERIFIED".equalsIgnoreCase(req.getVerificationStatus())) {
            existing.setVerifiedBy(username);
            existing.setVerifiedOn(OffsetDateTime.now());
        } else {
            existing.setVerifiedBy(null);
            existing.setVerifiedOn(null);
        }

        existing.setUpdatedBy(username);
        existing.setUpdatedOn(OffsetDateTime.now());

        return mapper.toBeneficiaryResponse(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        Beneficiary existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary", id));

        // Rule 30: Cannot delete beneficiary if allocations exist
        if (allocationRepository.existsByBeneficiaryId(id)) {
            throw new BusinessRuleException("Beneficiary has existing allocations and cannot be deleted. Deactivate or un-verify instead.");
        }

        repository.delete(existing);
    }
}
