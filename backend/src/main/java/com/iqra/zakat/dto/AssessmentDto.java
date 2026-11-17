package com.iqra.zakat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AssessmentDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotNull(message = "Zakat Year is required")
        private Integer zakatYear;

        @NotNull(message = "Zakat Amount is required")
        @Min(value = 0, message = "Zakat Amount cannot be negative")
        private BigDecimal zakatAmount;

        @NotNull(message = "Carry Forward is required")
        private BigDecimal carryForward;

        private String status; // OPEN, CLOSED
        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long assessmentId;
        private Integer zakatYear;
        private BigDecimal zakatAmount;
        private BigDecimal carryForward;
        private BigDecimal netPayable;
        private String status;
        private String remarks;
        private BigDecimal totalAllocatedAmount;
        private BigDecimal remainingBalance;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }
}
