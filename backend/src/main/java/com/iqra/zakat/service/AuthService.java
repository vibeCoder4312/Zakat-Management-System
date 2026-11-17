package com.iqra.zakat.service;

import com.iqra.zakat.dto.AuthDto;
import com.iqra.zakat.entity.AppUser;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.exception.ResourceNotFoundException;
import com.iqra.zakat.repository.AppUserRepository;
import com.iqra.zakat.security.JwtTokenProvider;
import com.iqra.zakat.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static class LoginAttempt {
        int attempts;
        Instant lockedUntil;

        LoginAttempt(int attempts) {
            this.attempts = attempts;
        }
    }

    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        String normalizedUsername = request.getUsername() != null ? request.getUsername().trim().toLowerCase() : "";

        LoginAttempt existingAttempt = loginAttempts.get(normalizedUsername);
        if (existingAttempt != null && existingAttempt.lockedUntil != null) {
            if (Instant.now().isBefore(existingAttempt.lockedUntil)) {
                long minutesLeft = Math.max(1, Duration.between(Instant.now(), existingAttempt.lockedUntil).toMinutes() + 1);
                log.warn("Login attempt for locked username: {}", normalizedUsername);
                throw new BusinessRuleException("TOO_MANY_ATTEMPTS",
                        "Too many failed login attempts. Account temporarily locked. Please try again in " + minutesLeft + " minutes.");
            } else {
                loginAttempts.remove(normalizedUsername);
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            // Clear attempts on success
            loginAttempts.remove(normalizedUsername);
        } catch (BadCredentialsException ex) {
            LoginAttempt current = loginAttempts.compute(normalizedUsername, (k, v) -> {
                if (v == null || (v.lockedUntil != null && Instant.now().isAfter(v.lockedUntil))) {
                    return new LoginAttempt(1);
                }
                v.attempts++;
                if (v.attempts >= 5) {
                    v.lockedUntil = Instant.now().plus(15, ChronoUnit.MINUTES);
                }
                return v;
            });

            if (current.lockedUntil != null && Instant.now().isBefore(current.lockedUntil)) {
                log.warn("User {} locked out after 5 failed login attempts.", normalizedUsername);
                throw new BusinessRuleException("TOO_MANY_ATTEMPTS",
                        "Too many failed login attempts. Account locked for 15 minutes.");
            }
            throw ex;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return AuthDto.LoginResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .userId(principal.getUserId())
                .username(principal.getUsername())
                .fullName(principal.getFullName())
                .role(principal.getRole())
                .build();
    }

    @Transactional
    public void changePassword(AuthDto.ChangePasswordRequest req) {
        String currentUsername = getCurrentUsername();
        if ("SYSTEM".equalsIgnoreCase(currentUsername)) {
            throw new BusinessRuleException("UNAUTHORIZED", "User must be authenticated to change password.");
        }

        AppUser user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("INVALID_PASSWORD", "Current password does not match.");
        }

        if (passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash()) ||
                req.getNewPassword().equals(req.getCurrentPassword())) {
            throw new BusinessRuleException("SAME_PASSWORD", "New password cannot be the same as the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedBy(currentUsername);
        user.setUpdatedOn(OffsetDateTime.now());
        userRepository.save(user);
        log.info("Password successfully changed for user: {}", currentUsername);
    }

    @Transactional
    public AuthDto.UserDto updateCurrentProfilePicture(String pictureData) {
        String currentUsername = getCurrentUsername();
        if ("SYSTEM".equalsIgnoreCase(currentUsername)) {
            throw new BusinessRuleException("UNAUTHORIZED", "User must be authenticated to update their profile photo.");
        }

        AppUser user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUsername));

        if (pictureData == null || pictureData.isBlank()) {
            user.setProfilePicture(null);
        } else {
            String mimeType;
            if (pictureData.startsWith("data:image/png;base64,")) mimeType = "image/png";
            else if (pictureData.startsWith("data:image/jpeg;base64,")) mimeType = "image/jpeg";
            else if (pictureData.startsWith("data:image/webp;base64,")) mimeType = "image/webp";
            else throw new BusinessRuleException("INVALID_PROFILE_PHOTO", "Use a PNG, JPEG, or WebP image.");

            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(pictureData.substring(pictureData.indexOf(',') + 1));
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleException("INVALID_PROFILE_PHOTO", "The selected profile photo is invalid.");
            }
            if (bytes.length == 0 || bytes.length > 2 * 1024 * 1024 || !hasValidImageSignature(bytes, mimeType)) {
                throw new BusinessRuleException("INVALID_PROFILE_PHOTO", "The photo must be a valid PNG, JPEG, or WebP image no larger than 2 MB.");
            }
            user.setProfilePicture(pictureData);
        }

        user.setUpdatedBy(currentUsername);
        user.setUpdatedOn(OffsetDateTime.now());
        return toUserDto(userRepository.save(user));
    }

    private boolean hasValidImageSignature(byte[] bytes, String mimeType) {
        if ("image/png".equals(mimeType)) {
            return bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47 && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
        }
        if ("image/jpeg".equals(mimeType)) {
            return bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
        }
        return bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    public AuthDto.UserDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .map(this::toUserDto)
                .orElseGet(() -> AuthDto.UserDto.builder()
                        .userId(principal.getUserId())
                        .username(principal.getUsername())
                        .fullName(principal.getFullName())
                        .role(principal.getRole())
                        .isActive("Y")
                        .build());
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    public AuthDto.UserDto toUserDto(AppUser user) {
        if (user == null) return null;
        return AuthDto.UserDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdBy(user.getCreatedBy())
                .createdOn(user.getCreatedOn())
                .updatedBy(user.getUpdatedBy())
                .updatedOn(user.getUpdatedOn())
                .build();
    }
}
