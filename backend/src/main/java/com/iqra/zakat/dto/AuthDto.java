package com.iqra.zakat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

public class AuthDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private String tokenType;
        private Long userId;
        private String username;
        private String fullName;
        private String email;
        private String phone;
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long userId;
        private String username;
        private String fullName;

        @Email(message = "Email address is invalid")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        private String email;

        @Size(max = 30, message = "Phone cannot exceed 30 characters")
        private String phone;
        private String profilePicture;
        private String role;
        private String isActive;
        private String createdBy;
        private OffsetDateTime createdOn;
        private String updatedBy;
        private OffsetDateTime updatedOn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfilePictureRequest {
        @Size(max = 2_800_000, message = "Profile photo must be 2 MB or smaller")
        private String profilePicture;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        private String fullName;

        @Email(message = "Email address is invalid")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        private String email;

        @Size(max = 30, message = "Phone cannot exceed 30 characters")
        private String phone;

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(ADMIN|ACCOUNTANT|VIEWER)$", message = "Role must be ADMIN, ACCOUNTANT, or VIEWER")
        private String role;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        private String password;

        @NotBlank(message = "isActive is required")
        @Pattern(regexp = "^(Y|N)$", message = "isActive must be 'Y' or 'N'")
        private String isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserRequest {
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        private String fullName;

        @Email(message = "Email address is invalid")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        private String email;

        @Size(max = 30, message = "Phone cannot exceed 30 characters")
        private String phone;

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(ADMIN|ACCOUNTANT|VIEWER)$", message = "Role must be ADMIN, ACCOUNTANT, or VIEWER")
        private String role;

        @NotBlank(message = "isActive is required")
        @Pattern(regexp = "^(Y|N)$", message = "isActive must be 'Y' or 'N'")
        private String isActive;

        @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
        private String newPassword;
    }
}
