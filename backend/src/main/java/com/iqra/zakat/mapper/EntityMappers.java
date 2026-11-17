package com.iqra.zakat.mapper;

import com.iqra.zakat.dto.*;
import com.iqra.zakat.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntityMappers {

    // --- Assessment ---
    public AssessmentDto.Response toAssessmentResponse(Assessment entity) {
        if (entity == null) return null;
        return AssessmentDto.Response.builder()
                .assessmentId(entity.getAssessmentId())
                .zakatYear(entity.getZakatYear())
                .zakatAmount(entity.getZakatAmount())
                .carryForward(entity.getCarryForward())
                .netPayable(entity.getNetPayable())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .totalAllocatedAmount(entity.getTotalAllocatedAmount())
                .remainingBalance(entity.getRemainingBalance())
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    public Assessment toAssessmentEntity(AssessmentDto.Request req) {
        if (req == null) return null;
        BigDecimal amt = req.getZakatAmount() != null ? req.getZakatAmount() : BigDecimal.ZERO;
        BigDecimal cf = req.getCarryForward() != null ? req.getCarryForward() : BigDecimal.ZERO;
        return Assessment.builder()
                .zakatYear(req.getZakatYear())
                .zakatAmount(amt)
                .carryForward(cf)
                .netPayable(amt.subtract(cf))
                .status(req.getStatus() != null ? req.getStatus() : "OPEN")
                .remarks(req.getRemarks())
                .totalAllocatedAmount(BigDecimal.ZERO)
                .remainingBalance(amt.subtract(cf))
                .build();
    }

    // --- Classification ---
    public MasterDtos.ClassificationResponse toClassificationResponse(Classification entity) {
        if (entity == null) return null;
        return MasterDtos.ClassificationResponse.builder()
                .classificationId(entity.getClassificationId())
                .code(entity.getCode())
                .name(entity.getName())
                .fundType(entity.getFundType())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    public Classification toClassificationEntity(MasterDtos.ClassificationRequest req) {
        if (req == null) return null;
        return Classification.builder()
                .code(req.getCode())
                .name(req.getName())
                .fundType(req.getFundType())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 99)
                .isActive(req.getIsActive() != null ? req.getIsActive() : "Y")
                .build();
    }

    // --- Beneficiary Category ---
    public MasterDtos.CategoryResponse toCategoryResponse(BeneficiaryCategory entity, Long beneficiaryCount) {
        if (entity == null) return null;
        return MasterDtos.CategoryResponse.builder()
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .isActive(entity.getIsActive())
                .beneficiaryCount(beneficiaryCount != null ? beneficiaryCount : 0L)
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    public BeneficiaryCategory toCategoryEntity(MasterDtos.CategoryRequest req) {
        if (req == null) return null;
        return BeneficiaryCategory.builder()
                .name(req.getName())
                .isActive(req.getIsActive() != null ? req.getIsActive() : "Y")
                .build();
    }

    // --- Source Account ---
    public MasterDtos.SourceAccountResponse toSourceAccountResponse(SourceAccount entity) {
        if (entity == null) return null;
        return MasterDtos.SourceAccountResponse.builder()
                .sourceAccountId(entity.getSourceAccountId())
                .name(entity.getName())
                .region(entity.getRegion())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    public SourceAccount toSourceAccountEntity(MasterDtos.SourceAccountRequest req) {
        if (req == null) return null;
        return SourceAccount.builder()
                .name(req.getName())
                .region(req.getRegion())
                .isActive(req.getIsActive() != null ? req.getIsActive() : "Y")
                .build();
    }

    // --- Agent ---
    public MasterDtos.AgentResponse toAgentResponse(Agent entity) {
        if (entity == null) return null;
        return MasterDtos.AgentResponse.builder()
                .agentId(entity.getAgentId())
                .name(entity.getName())
                .agentType(entity.getAgentType())
                .contactNumber(entity.getContactNumber())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    public Agent toAgentEntity(MasterDtos.AgentRequest req) {
        if (req == null) return null;
        return Agent.builder()
                .name(req.getName())
                .agentType(req.getAgentType())
                .contactNumber(req.getContactNumber())
                .isActive(req.getIsActive() != null ? req.getIsActive() : "Y")
                .build();
    }

    // --- Employee ---
    public MasterDtos.EmployeeResponse toEmployeeResponse(Employee entity) {
        if (entity == null) return null;
        return MasterDtos.EmployeeResponse.builder()
                .employeeId(entity.getEmployeeId())
                .name(entity.getName())
                .contactNumber(entity.getContactNumber())
                .remarks(entity.getRemarks())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    public Employee toEmployeeEntity(MasterDtos.EmployeeRequest req) {
        if (req == null) return null;
        return Employee.builder()
                .name(req.getName())
                .contactNumber(req.getContactNumber())
                .remarks(req.getRemarks())
                .isActive(req.getIsActive() != null ? req.getIsActive() : "Y")
                .build();
    }

    // --- School / Institute ---
    public MasterDtos.SchoolResponse toSchoolResponse(School entity, Long count) {
        if (entity == null) return null;
        return MasterDtos.SchoolResponse.builder()
                .schoolId(entity.getSchoolId())
                .name(entity.getName())
                .location(entity.getLocation())
                .isActive(entity.getIsActive())
                .beneficiaryCount(count != null ? count : 0L)
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    public School toSchoolEntity(MasterDtos.SchoolRequest req) {
        if (req == null) return null;
        return School.builder()
                .name(req.getName())
                .location(req.getLocation())
                .isActive(req.getIsActive() != null ? req.getIsActive() : "Y")
                .build();
    }

    // --- Beneficiary ---
    public BeneficiaryDto.Response toBeneficiaryResponse(Beneficiary entity) {
        if (entity == null) return null;
        return BeneficiaryDto.Response.builder()
                .beneficiaryId(entity.getBeneficiaryId())
                .beneficiaryCode(entity.getBeneficiaryCode())
                .name(entity.getName())
                .beneficiaryCategory(entity.getBeneficiaryCategory())
                .zakatYear(entity.getZakatYear())
                .contactNumber(entity.getContactNumber())
                .address(entity.getAddress())
                .description(entity.getDescription())
                .referredBy(entity.getReferredBy())
                .verificationStatus(entity.getVerificationStatus())
                .verifiedBy(entity.getVerifiedBy())
                .verifiedOn(entity.getVerifiedOn())
                .verificationNote(entity.getVerificationNote())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }

    public Beneficiary toBeneficiaryEntity(BeneficiaryDto.Request req) {
        if (req == null) return null;
        return Beneficiary.builder()
                .beneficiaryCode(req.getBeneficiaryCode())
                .name(req.getName())
                .beneficiaryCategory(req.getBeneficiaryCategory())
                .zakatYear(req.getZakatYear())
                .contactNumber(req.getContactNumber())
                .address(req.getAddress())
                .description(req.getDescription())
                .referredBy(req.getReferredBy())
                .verificationStatus(req.getVerificationStatus() != null ? req.getVerificationStatus() : "PENDING")
                .verificationNote(req.getVerificationNote())
                .isActive(req.getIsActive() != null ? req.getIsActive() : "Y")
                .build();
    }

    // --- Allocation ---
    public AllocationDto.Response toAllocationResponse(Allocation entity) {
        if (entity == null) return null;

        BigDecimal planned = entity.getPlannedAmount() != null ? entity.getPlannedAmount() : BigDecimal.ZERO;
        BigDecimal paid = entity.getPaidAmount() != null ? entity.getPaidAmount() : BigDecimal.ZERO;

        BigDecimal committed;
        if ("PAID".equals(entity.getStatus())) {
            committed = paid;
        } else if ("PLANNED".equals(entity.getStatus())) {
            committed = planned;
        } else {
            committed = BigDecimal.ZERO;
        }

        BigDecimal outstanding = "CANCELLED".equals(entity.getStatus()) ? BigDecimal.ZERO : planned.subtract(paid).max(BigDecimal.ZERO);

        String clsName = entity.getClassification();
        String fundType = null;
        if (entity.getClassificationEntity() != null) {
            clsName = entity.getClassificationEntity().getName();
            fundType = entity.getClassificationEntity().getFundType();
        }

        String benName = entity.getBeneficiary();
        String benCat = null;
        if (entity.getBeneficiaryEntity() != null) {
            benName = entity.getBeneficiaryEntity().getName();
            benCat = entity.getBeneficiaryEntity().getBeneficiaryCategory();
        }

        String schName = entity.getSchool();
        if (entity.getSchoolEntity() != null) {
            schName = entity.getSchoolEntity().getName();
        }

        String srcName = entity.getPaidSourceAccount();
        if (entity.getPaidSourceAccountEntity() != null) {
            srcName = entity.getPaidSourceAccountEntity().getName();
        }

        String agtName = entity.getPaidViaAgent();
        if (entity.getPaidViaAgentEntity() != null) {
            agtName = entity.getPaidViaAgentEntity().getName();
        }

        return AllocationDto.Response.builder()
                .allocationId(entity.getAllocationId())
                .assessmentId(entity.getAssessmentId())
                .zakatYear(entity.getZakatYear())
                .classificationId(entity.getClassificationId())
                .classification(clsName)
                .fundType(fundType)
                .beneficiaryId(entity.getBeneficiaryId())
                .beneficiary(benName)
                .beneficiaryCategory(benCat)
                .plannedAmount(planned)
                .schoolId(entity.getSchoolId())
                .school(schName)
                .currentStd(entity.getCurrentStd())
                .prevStdResult(entity.getPrevStdResult())
                .paidAmount(paid)
                .committedAmount(committed)
                .outstandingAmount(outstanding)
                .paidOn(entity.getPaidOn())
                .paymentMethod(entity.getPaymentMethod())
                .paidSourceAccountId(entity.getPaidSourceAccountId())
                .paidSourceAccount(srcName)
                .paidViaAgentId(entity.getPaidViaAgentId())
                .paidViaAgent(agtName)
                .paidViaEmployee(entity.getPaidViaEmployee())
                .referenceNo(entity.getReferenceNo())
                .currencyCode(entity.getCurrencyCode())
                .foreignAmount(entity.getForeignAmount())
                .fxRate(entity.getFxRate())
                .receiptReceived(entity.getReceiptReceived())
                .status(entity.getStatus())
                .approvalStatus(entity.getApprovalStatus())
                .category(entity.getCategory())
                .remarks(entity.getRemarks())
                .createdBy(entity.getCreatedBy())
                .createdOn(entity.getCreatedOn())
                .updatedBy(entity.getUpdatedBy())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }
}
