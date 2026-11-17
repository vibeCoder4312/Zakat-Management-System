package com.iqra.zakat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

public class BeneficiaryDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String beneficiaryCode;

        @NotBlank(message = "Name is required")
        private String name;

        private String beneficiaryCategory;
        private Integer zakatYear;
        private String contactNumber;
        private String address;
        private String description;
        private String referredBy;
        private String verificationStatus; // PENDING, VERIFIED, CANCELLED
        private String verificationNote;
        private String isActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long beneficiaryId;
        private String beneficiaryCode;
        private String name;
        private String beneficiaryCategory;
        private Integer zakatYear;
        private String contactNumber;
        private String address;
        private String description;
        private String referredBy;
        private String verificationStatus;
        private String verifiedBy;
        private OffsetDateTime verifiedOn;
        private String verificationNote;
        private String isActive;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationRequest {
        @NotBlank(message = "Verification status is required")
        private String verificationStatus; // PENDING, VERIFIED, CANCELLED
        private String verificationNote;
    }
}
