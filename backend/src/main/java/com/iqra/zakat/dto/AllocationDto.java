package com.iqra.zakat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class AllocationDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotNull(message = "Zakat Year is required")
        private Integer zakatYear;

        @NotNull(message = "Classification is required")
        private Long classificationId;

        @NotNull(message = "Beneficiary is required")
        private Long beneficiaryId;

        @NotNull(message = "Planned Amount is required")
        @Min(value = 0, message = "Planned Amount cannot be negative")
        private BigDecimal plannedAmount;

        private Long schoolId;
        private String currentStd;
        private String prevStdResult;

        @Min(value = 0, message = "Paid Amount cannot be negative")
        private BigDecimal paidAmount;

        private LocalDate paidOn;
        private String paymentMethod; // CASH, ONLINE, BANK
        private Long paidSourceAccountId;
        private String currencyCode; // INR, AED
        private BigDecimal foreignAmount;
        private BigDecimal fxRate;
        private String receiptReceived; // Y, N
        private String status; // PLANNED, PAID, CANCELLED
        private String approvalStatus; // PENDING, APPROVED
        private String category;
        private String paidViaAgent;
        private String paidViaEmployee;
        private String referenceNo;
        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long allocationId;
        private Long assessmentId;
        private Integer zakatYear;
        private Long classificationId;
        private String classification;
        private String fundType;
        private Long beneficiaryId;
        private String beneficiary;
        private String beneficiaryCategory;
        private BigDecimal plannedAmount;
        private Long schoolId;
        private String school;
        private String currentStd;
        private String prevStdResult;
        private BigDecimal paidAmount;
        private BigDecimal committedAmount;
        private BigDecimal outstandingAmount;
        private LocalDate paidOn;
        private String paymentMethod;
        private Long paidSourceAccountId;
        private String paidSourceAccount;
        private Long paidViaAgentId;
        private String paidViaAgent;
        private String paidViaEmployee;
        private String referenceNo;
        private String currencyCode;
        private BigDecimal foreignAmount;
        private BigDecimal fxRate;
        private String receiptReceived;
        private String status;
        private String approvalStatus;
        private String category;
        private String remarks;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryResponse {
        private long totalAllocations;
        private BigDecimal totalAllocated;
        private BigDecimal totalPaid;
        private BigDecimal remainingAmount; // outstanding amount
    }
}
