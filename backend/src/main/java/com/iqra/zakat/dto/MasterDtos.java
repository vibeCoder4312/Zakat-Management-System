package com.iqra.zakat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

public class MasterDtos {

    // --- Classification ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassificationRequest {
        private String code;
        @NotBlank(message = "Name is required")
        private String name;
        @NotBlank(message = "Fund Type is required")
        @Pattern(regexp = "ZAKAT|SADAQAH", message = "Fund Type must be ZAKAT or SADAQAH")
        private String fundType;
        private Integer displayOrder;
        private String isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassificationResponse {
        private Long classificationId;
        private String code;
        private String name;
        private String fundType;
        private Integer displayOrder;
        private String isActive;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }

    // --- Beneficiary Category ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryRequest {
        @NotBlank(message = "Name is required")
        private String name;
        private String isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryResponse {
        private Long categoryId;
        private String name;
        private String isActive;
        private Long beneficiaryCount;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }

    // --- Source Account ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceAccountRequest {
        @NotBlank(message = "Name is required")
        private String name;
        @NotBlank(message = "Region is required")
        @Pattern(regexp = "INDIA|DUBAI", message = "Region must be INDIA or DUBAI")
        private String region;
        private String isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceAccountResponse {
        private Long sourceAccountId;
        private String name;
        private String region;
        private String isActive;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }

    // --- Agent ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgentRequest {
        @NotBlank(message = "Name is required")
        private String name;
        @NotBlank(message = "Agent Type is required")
        @Pattern(regexp = "EMPLOYEE|PARTNER", message = "Agent Type must be EMPLOYEE or PARTNER")
        private String agentType;
        private String contactNumber;
        private String isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgentResponse {
        private Long agentId;
        private String name;
        private String agentType;
        private String contactNumber;
        private String isActive;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }

    // --- Employee ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeRequest {
        @NotBlank(message = "Name is required")
        private String name;
        private String contactNumber;
        private String remarks;
        private String isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeResponse {
        private Long employeeId;
        private String name;
        private String contactNumber;
        private String remarks;
        private String isActive;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }

    // --- School / Institute ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchoolRequest {
        @NotBlank(message = "Name is required")
        private String name;
        private String location;
        private String isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchoolResponse {
        private Long schoolId;
        private String name;
        private String location;
        private String isActive;
        private Long beneficiaryCount;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }
}
