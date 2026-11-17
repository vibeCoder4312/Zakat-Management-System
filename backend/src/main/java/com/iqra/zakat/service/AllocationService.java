package com.iqra.zakat.service;

import com.iqra.zakat.dto.AllocationDto;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.entity.*;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.exception.ResourceNotFoundException;
import com.iqra.zakat.mapper.EntityMappers;
import com.iqra.zakat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final AssessmentRepository assessmentRepository;
    private final ClassificationRepository classificationRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SchoolRepository schoolRepository;
    private final SourceAccountRepository sourceAccountRepository;
    private final AgentRepository agentRepository;
    private final AssessmentService assessmentService;
    private final EntityMappers mapper;
    private final AuthService authService;

    public record ReceiptDownload(String filename, String contentType, byte[] bytes) {}

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "allocationId", "zakatYear", "beneficiary", "classification", "category",
            "plannedAmount", "paidAmount", "status", "approvalStatus", "school",
            "paidSourceAccount", "paymentMethod", "paidOn", "receiptReceived", "createdOn", "updatedOn"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    @Transactional(readOnly = true)
    public PagedResponse<AllocationDto.Response> list(
            int page, int size, String sort,
            List<Integer> years, Long classificationId, String category,
            String status, String approvalStatus, Long beneficiaryId,
            Long sourceAccountId, Long schoolId, String paymentMethod, String q) {

        Sort sortObj = Sort.by(Sort.Direction.DESC, "allocationId");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            if (ALLOWED_SORT_FIELDS.contains(field)) {
                Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
                sortObj = Sort.by(dir, field).and(Sort.by(Sort.Direction.DESC, "allocationId")); // stable paging
            }
        }
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Allocation> result = allocationRepository.searchAllocations(
                years != null && years.isEmpty() ? null : years,
                classificationId, category, status, approvalStatus,
                beneficiaryId, sourceAccountId, schoolId, paymentMethod, q, pageable);

        return PagedResponse.<AllocationDto.Response>builder()
                .content(result.getContent().stream().map(mapper::toAllocationResponse).toList())
                .pageNumber(result.getNumber())
                .pageSize(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AllocationDto.SummaryResponse getSummary(
            List<Integer> years, Long classificationId, String category,
            String status, String approvalStatus, Long beneficiaryId,
            Long sourceAccountId, Long schoolId, String paymentMethod, String q) {

        List<Object[]> rows = allocationRepository.getFilteredSummary(
                years != null && years.isEmpty() ? null : years,
                classificationId, category, status, approvalStatus,
                beneficiaryId, sourceAccountId, schoolId, paymentMethod, q);

        if (rows.isEmpty() || rows.get(0) == null) {
            return AllocationDto.SummaryResponse.builder()
                    .totalAllocations(0L)
                    .totalAllocated(BigDecimal.ZERO)
                    .totalPaid(BigDecimal.ZERO)
                    .remainingAmount(BigDecimal.ZERO)
                    .build();
        }

        Object[] r = rows.get(0);
        long count = r[0] != null ? ((Number) r[0]).longValue() : 0L;
        BigDecimal allocated = r[1] != null ? (BigDecimal) r[1] : BigDecimal.ZERO;
        BigDecimal paid = r[2] != null ? (BigDecimal) r[2] : BigDecimal.ZERO;
        BigDecimal remaining = r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO;

        return AllocationDto.SummaryResponse.builder()
                .totalAllocations(count)
                .totalAllocated(allocated)
                .totalPaid(paid)
                .remainingAmount(remaining)
                .build();
    }

    @Transactional(readOnly = true)
    public AllocationDto.Response getById(Long id) {
        return mapper.toAllocationResponse(allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id)));
    }

    @Transactional
    public AllocationDto.Response create(AllocationDto.Request req) {
        // Validation Rule 13: Resolve assessment
        if (req.getZakatYear() == null) {
            throw new BusinessRuleException("Select a Zakat Year for this allocation.");
        }
        Assessment assessment = assessmentRepository.findByZakatYear(req.getZakatYear())
                .orElseThrow(() -> new BusinessRuleException("Select a Zakat Year for this allocation."));

        // Rule 6: CLOSED year check
        if ("CLOSED".equalsIgnoreCase(assessment.getStatus())) {
            throw new BusinessRuleException("This year is CLOSED. Re-open the Zakat year to change allocations or payments.");
        }

        // Validation Rule 14: Beneficiary check
        if (req.getBeneficiaryId() == null) {
            throw new BusinessRuleException("Select a beneficiary that exists in the Beneficiaries master.");
        }
        Beneficiary ben = beneficiaryRepository.findById(req.getBeneficiaryId())
                .orElseThrow(() -> new BusinessRuleException("Select a beneficiary that exists in the Beneficiaries master."));

        // Validation Rule 15: Beneficiary must be VERIFIED and active
        if (!"VERIFIED".equalsIgnoreCase(ben.getVerificationStatus()) || !"Y".equalsIgnoreCase(ben.getIsActive())) {
            throw new BusinessRuleException("Beneficiary must be Verified and Active before an allocation can be made.");
        }

        // Classification
        Classification cls = classificationRepository.findById(req.getClassificationId())
                .orElseThrow(() -> new ResourceNotFoundException("Classification", req.getClassificationId()));

        Allocation entity = new Allocation();
        entity.setAssessmentId(assessment.getAssessmentId());
        entity.setZakatYear(assessment.getZakatYear());
        entity.setClassificationId(cls.getClassificationId());
        entity.setClassification(cls.getName());
        entity.setBeneficiaryId(ben.getBeneficiaryId());
        entity.setBeneficiary(ben.getName());
        // Read-only associations: set so the response carries fund type and beneficiary category
        entity.setClassificationEntity(cls);
        entity.setBeneficiaryEntity(ben);

        entity.setPlannedAmount(req.getPlannedAmount() != null ? req.getPlannedAmount() : BigDecimal.ZERO);

        // School
        if (req.getSchoolId() != null) {
            School sch = schoolRepository.findById(req.getSchoolId()).orElse(null);
            entity.setSchoolId(req.getSchoolId());
            if (sch != null) entity.setSchool(sch.getName());
        }
        entity.setCurrentStd(req.getCurrentStd());
        entity.setPrevStdResult(req.getPrevStdResult());

        // Currency and Paid Amount (Rule 16 & 17)
        String currency = req.getCurrencyCode() != null && !req.getCurrencyCode().isBlank() ? req.getCurrencyCode() : "INR";
        entity.setCurrencyCode(currency);
        entity.setForeignAmount(req.getForeignAmount());
        entity.setFxRate(req.getFxRate());

        BigDecimal paidAmt = req.getPaidAmount() != null ? req.getPaidAmount() : BigDecimal.ZERO;
        if (!"INR".equals(currency) && req.getForeignAmount() != null && req.getFxRate() != null) {
            paidAmt = req.getForeignAmount().multiply(req.getFxRate()).setScale(2, RoundingMode.HALF_UP);
        }
        entity.setPaidAmount(paidAmt);

        // Status logic (Rule 18)
        String requestedStatus = req.getStatus();
        if ("CANCELLED".equalsIgnoreCase(requestedStatus)) {
            if (paidAmt.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessRuleException("A payment is recorded on this allocation. Clear the paid amount before cancelling.");
            }
            entity.setStatus("CANCELLED");
        } else if (paidAmt.compareTo(BigDecimal.ZERO) > 0) {
            entity.setStatus("PAID");
        } else {
            entity.setStatus("PLANNED");
        }

        // Approval status logic (Rule 22)
        if (paidAmt.compareTo(BigDecimal.ZERO) > 0) {
            entity.setApprovalStatus("APPROVED");
        } else {
            entity.setApprovalStatus(req.getApprovalStatus() != null ? req.getApprovalStatus() : "PENDING");
        }

        // Paid On & Payment Method (Rules 19 & 20 - do NOT default)
        entity.setPaidOn(req.getPaidOn());
        entity.setPaymentMethod(req.getPaymentMethod());

        // Source Account
        if (req.getPaidSourceAccountId() != null) {
            SourceAccount sa = sourceAccountRepository.findById(req.getPaidSourceAccountId()).orElse(null);
            entity.setPaidSourceAccountId(req.getPaidSourceAccountId());
            if (sa != null) entity.setPaidSourceAccount(sa.getName());
        }

        // Agent / Employee (Rule 25)
        entity.setPaidViaAgent(req.getPaidViaAgent());
        if (req.getPaidViaAgent() != null && !req.getPaidViaAgent().isBlank()) {
            agentRepository.findByNameIgnoreCase(req.getPaidViaAgent().trim())
                    .ifPresent(agent -> entity.setPaidViaAgentId(agent.getAgentId()));
        }
        entity.setPaidViaEmployee(req.getPaidViaEmployee());

        // Receipt (Rule 21)
        entity.setReceiptReceived(req.getReceiptReceived() != null ? req.getReceiptReceived() : "N");

        // Category drives Planning matching (year + classification + category). Default it to the
        // beneficiary's category when the form leaves it blank, as the APEX form did.
        entity.setCategory(req.getCategory() != null && !req.getCategory().isBlank()
                ? req.getCategory().trim() : ben.getBeneficiaryCategory());
        entity.setReferenceNo(req.getReferenceNo());
        entity.setRemarks(req.getRemarks());

        String username = authService.getCurrentUsername();
        entity.setCreatedBy(username);
        entity.setCreatedOn(OffsetDateTime.now());
        entity.setUpdatedBy(username);
        entity.setUpdatedOn(OffsetDateTime.now());

        Allocation saved = allocationRepository.save(entity);

        // Recalculate Assessment Totals (Rule 4)
        assessmentService.recalculateTotals(assessment.getAssessmentId());

        return mapper.toAllocationResponse(saved);
    }

    @Transactional
    public AllocationDto.Response update(Long id, AllocationDto.Request req) {
        Allocation existing = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));

        // Rule 6: Check if current year is CLOSED
        Assessment currentAsm = assessmentRepository.findById(existing.getAssessmentId())
                .orElseThrow(() -> new BusinessRuleException("Assessment not found for allocation"));
        if ("CLOSED".equalsIgnoreCase(currentAsm.getStatus())) {
            throw new BusinessRuleException("This year is CLOSED. Re-open the Zakat year to change allocations or payments.");
        }

        // Capture previous assessment ID before potentially moving to a new year
        Long previousAssessmentId = existing.getAssessmentId();

        // If year is changing, check target year
        if (req.getZakatYear() != null && !req.getZakatYear().equals(existing.getZakatYear())) {
            Assessment newAsm = assessmentRepository.findByZakatYear(req.getZakatYear())
                    .orElseThrow(() -> new BusinessRuleException("Select a Zakat Year for this allocation."));
            if ("CLOSED".equalsIgnoreCase(newAsm.getStatus())) {
                throw new BusinessRuleException("Target year is CLOSED. Cannot move allocation to a CLOSED year.");
            }
            existing.setAssessmentId(newAsm.getAssessmentId());
            existing.setZakatYear(newAsm.getZakatYear());
        }

        // Beneficiary change validation (Rule 15)
        if (req.getBeneficiaryId() != null && !req.getBeneficiaryId().equals(existing.getBeneficiaryId())) {
            Beneficiary ben = beneficiaryRepository.findById(req.getBeneficiaryId())
                    .orElseThrow(() -> new BusinessRuleException("Select a beneficiary that exists in the Beneficiaries master."));
            if (!"VERIFIED".equalsIgnoreCase(ben.getVerificationStatus()) || !"Y".equalsIgnoreCase(ben.getIsActive())) {
                throw new BusinessRuleException("Beneficiary must be Verified and Active before an allocation can be made.");
            }
            existing.setBeneficiaryId(ben.getBeneficiaryId());
            existing.setBeneficiary(ben.getName());
        }

        // Classification
        if (req.getClassificationId() != null) {
            Classification cls = classificationRepository.findById(req.getClassificationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classification", req.getClassificationId()));
            existing.setClassificationId(cls.getClassificationId());
            existing.setClassification(cls.getName());
        }

        existing.setPlannedAmount(req.getPlannedAmount() != null ? req.getPlannedAmount() : BigDecimal.ZERO);

        // School
        if (req.getSchoolId() != null) {
            School sch = schoolRepository.findById(req.getSchoolId()).orElse(null);
            existing.setSchoolId(req.getSchoolId());
            if (sch != null) existing.setSchool(sch.getName());
        } else {
            existing.setSchoolId(null);
            existing.setSchool(null);
        }
        existing.setCurrentStd(req.getCurrentStd());
        existing.setPrevStdResult(req.getPrevStdResult());

        // Currency & FX
        String currency = req.getCurrencyCode() != null ? req.getCurrencyCode() : existing.getCurrencyCode();
        existing.setCurrencyCode(currency);
        existing.setForeignAmount(req.getForeignAmount());
        existing.setFxRate(req.getFxRate());

        BigDecimal paidAmt = req.getPaidAmount() != null ? req.getPaidAmount() : BigDecimal.ZERO;
        if (!"INR".equals(currency) && req.getForeignAmount() != null && req.getFxRate() != null) {
            paidAmt = req.getForeignAmount().multiply(req.getFxRate()).setScale(2, RoundingMode.HALF_UP);
        }
        existing.setPaidAmount(paidAmt);

        // Status transitions (Rule 18)
        String requestedStatus = req.getStatus();
        if ("CANCELLED".equalsIgnoreCase(requestedStatus)) {
            if (paidAmt.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessRuleException("A payment is recorded on this allocation. Clear the paid amount before cancelling.");
            }
            existing.setStatus("CANCELLED");
        } else if (paidAmt.compareTo(BigDecimal.ZERO) > 0) {
            existing.setStatus("PAID");
        } else {
            existing.setStatus("PLANNED");
        }

        // Approval status logic (Rule 22)
        if (paidAmt.compareTo(BigDecimal.ZERO) > 0) {
            existing.setApprovalStatus("APPROVED");
        } else if (req.getApprovalStatus() != null) {
            existing.setApprovalStatus(req.getApprovalStatus());
        }

        existing.setPaidOn(req.getPaidOn());
        existing.setPaymentMethod(req.getPaymentMethod());

        // Source Account
        if (req.getPaidSourceAccountId() != null) {
            SourceAccount sa = sourceAccountRepository.findById(req.getPaidSourceAccountId()).orElse(null);
            existing.setPaidSourceAccountId(req.getPaidSourceAccountId());
            if (sa != null) existing.setPaidSourceAccount(sa.getName());
        } else {
            existing.setPaidSourceAccountId(null);
            existing.setPaidSourceAccount(null);
        }

        // Agent / Employee
        existing.setPaidViaAgent(req.getPaidViaAgent());
        existing.setPaidViaAgentId(null);
        if (req.getPaidViaAgent() != null && !req.getPaidViaAgent().isBlank()) {
            agentRepository.findByNameIgnoreCase(req.getPaidViaAgent().trim())
                    .ifPresent(agent -> existing.setPaidViaAgentId(agent.getAgentId()));
        }
        existing.setPaidViaEmployee(req.getPaidViaEmployee());

        if (req.getReceiptReceived() != null) {
            existing.setReceiptReceived(req.getReceiptReceived());
        }

        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            existing.setCategory(req.getCategory().trim()); // keep historical category when not sent
        }
        existing.setReferenceNo(req.getReferenceNo());
        existing.setRemarks(req.getRemarks());

        existing.setUpdatedBy(authService.getCurrentUsername());
        existing.setUpdatedOn(OffsetDateTime.now());

        Allocation saved = allocationRepository.save(existing);

        // Recalculate totals for new/current assessment
        assessmentService.recalculateTotals(saved.getAssessmentId());

        // Recalculate previous assessment totals if the allocation moved across years
        if (previousAssessmentId != null && !previousAssessmentId.equals(saved.getAssessmentId())) {
            assessmentService.recalculateTotals(previousAssessmentId);
        }

        return mapper.toAllocationResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Allocation existing = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));

        // Rule 6: Check CLOSED year
        Assessment asm = assessmentRepository.findById(existing.getAssessmentId()).orElse(null);
        if (asm != null && "CLOSED".equalsIgnoreCase(asm.getStatus())) {
            throw new BusinessRuleException("This year is CLOSED. Re-open the Zakat year to change allocations or payments.");
        }

        Long assessmentId = existing.getAssessmentId();
        allocationRepository.delete(existing);

        // Recalculate Assessment Totals (Rule 4)
        assessmentService.recalculateTotals(assessmentId);
    }

    @Transactional
    public void uploadReceipt(Long id, byte[] fileBytes, String filename, String mimetype) {
        Allocation existing = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));

        // Check if allocation's assessment year is CLOSED
        Assessment asm = assessmentRepository.findById(existing.getAssessmentId()).orElse(null);
        if (asm != null && "CLOSED".equalsIgnoreCase(asm.getStatus())) {
            throw new BusinessRuleException("Cannot upload receipt for a CLOSED assessment year.");
        }

        // File validation
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BusinessRuleException("Receipt file cannot be empty.");
        }
        if (fileBytes.length > 10 * 1024 * 1024) {
            throw new BusinessRuleException("Receipt file cannot exceed 10 MB.");
        }

        // MIME validation against allowlist
        if (mimetype == null || !ALLOWED_MIME_TYPES.contains(mimetype.trim().toLowerCase())) {
            throw new BusinessRuleException("Invalid receipt file type: " + mimetype + ". Allowed types: PDF, JPEG, PNG, WEBP, GIF.");
        }

        // Sanitize filename: strip path characters, quotes, and CR/LF
        String sanitizedFilename = sanitizeFilename(filename, id);

        allocationRepository.updateReceiptFile(
                id,
                fileBytes,
                sanitizedFilename,
                mimetype.trim().toLowerCase(),
                authService.getCurrentUsername());
    }

    @Transactional(readOnly = true)
    public ReceiptDownload downloadReceipt(Long id) {
        Allocation existing = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", id));

        byte[] bytes = allocationRepository.findReceiptBytesById(id);
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        String filename = existing.getReceiptFilename() != null && !existing.getReceiptFilename().isBlank()
                ? existing.getReceiptFilename()
                : "receipt-" + id;
        String contentType = existing.getReceiptMimetype() != null && !existing.getReceiptMimetype().isBlank()
                ? existing.getReceiptMimetype()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return new ReceiptDownload(filename, contentType, bytes);
    }

    private String sanitizeFilename(String filename, Long id) {
        if (filename == null || filename.isBlank()) {
            return "receipt-" + id;
        }
        // Strip path traversal and directory separators, quotes, CR, LF
        String clean = filename.replaceAll("[/\\\\\"'\\r\\n]", "_").trim();
        clean = clean.replaceAll("^\\.+", "");
        return clean.isBlank() ? "receipt-" + id : clean;
    }
}
