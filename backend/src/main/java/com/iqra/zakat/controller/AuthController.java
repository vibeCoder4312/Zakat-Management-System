package com.iqra.zakat.controller;

import com.iqra.zakat.dto.AuthDto;
import com.iqra.zakat.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ResponseEntity<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<AuthDto.UserDto> getCurrentUser() {
        AuthDto.UserDto user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Self-service password change for authenticated user")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    @PutMapping("/profile-picture")
    @Operation(summary = "Update or remove the current user's profile photo")
    public ResponseEntity<AuthDto.UserDto> updateProfilePicture(@Valid @RequestBody AuthDto.ProfilePictureRequest request) {
        return ResponseEntity.ok(authService.updateCurrentProfilePicture(request.getProfilePicture()));
    }
}
