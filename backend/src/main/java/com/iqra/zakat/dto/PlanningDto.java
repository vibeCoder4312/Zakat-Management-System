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

public class PlanningDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotNull(message = "Zakat Year is required")
        private Integer zakatYear;

        private Long classificationId;
        private String category;

        @NotNull(message = "Planned Amount is required")
        @Min(value = 0, message = "Planned Amount cannot be negative")
        private BigDecimal plannedAmount;

        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long planningId;
        private Integer zakatYear;
        private String planType; // ZAKAT, SADAQAH
        private Long classificationId;
        private String classification;
        private String category;
        private BigDecimal plannedAmount;

        // Live calculated figures from allocations
        private BigDecimal paidAmount;
        private BigDecimal remainingAmount;
        private BigDecimal outstandingAmount;
        private Long allocationCount;
        private Long beneficiaryCount;
        private String sourceAccount;
        private LocalDate lastPaymentDate;
        private String paymentStatus; // PLANNED, PARTIAL, PAID, CANCELLED

        // Prior year comparisons
        private BigDecimal cyMinus1;
        private BigDecimal cyMinus2;
        private BigDecimal cyMinus3;
        private BigDecimal cyMinus4;
        private BigDecimal cyMinus5;

        private String remarks;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }
}
