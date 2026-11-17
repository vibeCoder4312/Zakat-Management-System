package com.iqra.zakat.service;

import com.iqra.zakat.dto.AssessmentDto;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.entity.Assessment;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.exception.ResourceNotFoundException;
import com.iqra.zakat.mapper.EntityMappers;
import com.iqra.zakat.repository.AllocationRepository;
import com.iqra.zakat.repository.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AllocationRepository allocationRepository;
    private final EntityMappers mapper;
    private final AuthService authService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "assessmentId", "zakatYear", "zakatAmount", "carryForward", "netPayable",
            "status", "remarks", "totalAllocatedAmount", "remainingBalance", "createdOn", "updatedOn"
    );

    @Transactional(readOnly = true)
    public PagedResponse<AssessmentDto.Response> getAssessments(int page, int size, String sort, String q) {
        Sort sortObj = Sort.by(Sort.Direction.DESC, "zakatYear");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            if (ALLOWED_SORT_FIELDS.contains(field)) {
                Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
                sortObj = Sort.by(dir, field);
            }
        }
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Assessment> result = assessmentRepository.searchAssessments(q, pageable);

        List<AssessmentDto.Response> dtoList = result.getContent().stream()
                .map(mapper::toAssessmentResponse)
                .toList();

        return PagedResponse.<AssessmentDto.Response>builder()
                .content(dtoList)
                .pageNumber(result.getNumber())
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AssessmentDto.Response getAssessmentById(Long id) {
        Assessment a = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", id));
        return mapper.toAssessmentResponse(a);
    }

    @Transactional(readOnly = true)
    public AssessmentDto.Response getAssessmentByYear(Integer year) {
        Assessment a = assessmentRepository.findByZakatYear(year)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment for year " + year + " not found"));
        return mapper.toAssessmentResponse(a);
    }

    @Transactional(readOnly = true)
    public List<Integer> getDistinctYears() {
        return assessmentRepository.findAllDistinctYears();
    }

    @Transactional
    public AssessmentDto.Response createAssessment(AssessmentDto.Request req) {
        if (assessmentRepository.existsByZakatYear(req.getZakatYear())) {
            throw new BusinessRuleException("Assessment for year " + req.getZakatYear() + " already exists. Edit the existing assessment instead.");
        }

        Assessment entity = mapper.toAssessmentEntity(req);
        entity.setCreatedBy(authService.getCurrentUsername());
        entity.setCreatedOn(OffsetDateTime.now());
        entity.setUpdatedBy(authService.getCurrentUsername());
        entity.setUpdatedOn(OffsetDateTime.now());

        Assessment saved = assessmentRepository.save(entity);
        return mapper.toAssessmentResponse(saved);
    }

    @Transactional
    public AssessmentDto.Response updateAssessment(Long id, AssessmentDto.Request req) {
        Assessment existing = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", id));

        // Rule 7: Cannot edit Zakat amount or Carry forward when CLOSED
        if ("CLOSED".equalsIgnoreCase(existing.getStatus()) && !"OPEN".equalsIgnoreCase(req.getStatus())) {
            boolean amtChanged = req.getZakatAmount() != null && req.getZakatAmount().compareTo(existing.getZakatAmount()) != 0;
            boolean cfChanged = req.getCarryForward() != null && req.getCarryForward().compareTo(existing.getCarryForward()) != 0;
            if (amtChanged || cfChanged) {
                throw new BusinessRuleException("This Zakat year is CLOSED. Re-open it to change year, amount or carry-forward.");
            }
        }

        if (req.getZakatAmount() != null) existing.setZakatAmount(req.getZakatAmount());
        if (req.getCarryForward() != null) existing.setCarryForward(req.getCarryForward());
        if (req.getStatus() != null) existing.setStatus(req.getStatus());
        if (req.getRemarks() != null) existing.setRemarks(req.getRemarks());

        BigDecimal netPayable = existing.getZakatAmount().subtract(existing.getCarryForward());
        existing.setNetPayable(netPayable);
        BigDecimal totalAllocated = existing.getTotalAllocatedAmount() != null ? existing.getTotalAllocatedAmount() : BigDecimal.ZERO;
        existing.setRemainingBalance(netPayable.subtract(totalAllocated));

        existing.setUpdatedBy(authService.getCurrentUsername());
        existing.setUpdatedOn(OffsetDateTime.now());

        Assessment saved = assessmentRepository.save(existing);
        return mapper.toAssessmentResponse(saved);
    }

    @Transactional
    public void deleteAssessment(Long id) {
        Assessment a = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", id));

        // Rule 29: Cannot delete if allocations exist
        if (allocationRepository.existsByAssessmentId(id)) {
            throw new BusinessRuleException("Cannot delete assessment because allocations exist for this year. Deactivate or re-open instead.");
        }

        assessmentRepository.delete(a);
    }

    /**
     * Recalculates total allocated / net payable / remaining balance for one assessment.
     * Runs inside the caller's transaction (allocation create/update/delete), so the change and the
     * totals commit together. Implemented in JPQL rather than by calling the plpgsql function, because a
     * failing native call marks the whole transaction rollback-only.
     */
    @Transactional
    public void recalculateTotals(Long assessmentId) {
        Assessment asm = assessmentRepository.findByIdForUpdate(assessmentId)
                .or(() -> assessmentRepository.findById(assessmentId))
                .orElse(null);
        if (asm == null) {
            return;
        }
        BigDecimal totalAllocated = allocationRepository.sumZakatCommitted(assessmentId);
        if (totalAllocated == null) {
            totalAllocated = BigDecimal.ZERO;
        }
        BigDecimal netPayable = asm.getZakatAmount().subtract(asm.getCarryForward());
        asm.setNetPayable(netPayable);
        asm.setTotalAllocatedAmount(totalAllocated);
        asm.setRemainingBalance(netPayable.subtract(totalAllocated));
        assessmentRepository.save(asm);
    }
}
