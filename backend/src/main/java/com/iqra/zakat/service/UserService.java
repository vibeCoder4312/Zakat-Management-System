package com.iqra.zakat.service;

import com.iqra.zakat.dto.AuthDto;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.entity.AppUser;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.exception.ResourceNotFoundException;
import com.iqra.zakat.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public PagedResponse<AuthDto.UserDto> listUsers(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "username"));
        Page<AppUser> pageResult = userRepository.searchUsers(q != null && !q.isBlank() ? q.trim() : null, pageable);

        return PagedResponse.<AuthDto.UserDto>builder()
                .content(pageResult.getContent().stream().map(authService::toUserDto).toList())
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthDto.UserDto getUser(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return authService.toUserDto(user);
    }

    @Transactional
    public AuthDto.UserDto createUser(AuthDto.CreateUserRequest req) {
        String username = req.getUsername().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessRuleException("DUPLICATE_USERNAME", "Username already exists: " + username);
        }

        String currentUsername = authService.getCurrentUsername();
        OffsetDateTime now = OffsetDateTime.now();

        AppUser user = AppUser.builder()
                .username(username.toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName().trim())
                .email(normalizeOptional(req.getEmail()))
                .phone(normalizeOptional(req.getPhone()))
                .role(req.getRole())
                .isActive(req.getIsActive())
                .build();
        user.setCreatedBy(currentUsername);
        user.setCreatedOn(now);
        user.setUpdatedBy(currentUsername);
        user.setUpdatedOn(now);

        AppUser saved = userRepository.save(user);
        log.info("Admin created new user {} with role {}", saved.getUsername(), saved.getRole());
        return authService.toUserDto(saved);
    }

    @Transactional
    public AuthDto.UserDto updateUser(Long id, AuthDto.UpdateUserRequest req) {
        AppUser existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String currentUsername = authService.getCurrentUsername();

        // Safety Rule (a): an admin cannot deactivate or remove ADMIN role from their own currently logged-in account
        if (existing.getUsername().equalsIgnoreCase(currentUsername)) {
            if ("N".equalsIgnoreCase(req.getIsActive())) {
                throw new BusinessRuleException("SELF_DEACTIVATION", "Administrators cannot deactivate their own account.");
            }
            if (!"ADMIN".equals(req.getRole())) {
                throw new BusinessRuleException("SELF_DEMOTION", "Administrators cannot remove the ADMIN role from their own account.");
            }
        }

        // Safety Rule (b): an admin cannot deactivate or demote the last active ADMIN
        if ("ADMIN".equals(existing.getRole()) && "Y".equals(existing.getIsActive())) {
            boolean isDemoting = !"ADMIN".equals(req.getRole());
            boolean isDeactivating = "N".equalsIgnoreCase(req.getIsActive());
            if (isDemoting || isDeactivating) {
                long activeAdminCount = userRepository.countByRoleAndIsActive("ADMIN", "Y");
                if (activeAdminCount <= 1) {
                    throw new BusinessRuleException("LAST_ADMIN", "Cannot deactivate or demote the last active ADMIN.");
                }
            }
        }

        existing.setFullName(req.getFullName().trim());
        existing.setEmail(normalizeOptional(req.getEmail()));
        existing.setPhone(normalizeOptional(req.getPhone()));
        existing.setRole(req.getRole());
        existing.setIsActive(req.getIsActive());

        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        }

        existing.setUpdatedBy(currentUsername);
        existing.setUpdatedOn(OffsetDateTime.now());

        AppUser saved = userRepository.save(existing);
        log.info("Admin updated user {} (role: {}, isActive: {})", saved.getUsername(), saved.getRole(), saved.getIsActive());
        return authService.toUserDto(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        AppUser existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String currentUsername = authService.getCurrentUsername();

        // Safety Rule (a): Cannot delete own account
        if (existing.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new BusinessRuleException("SELF_DELETION", "Administrators cannot delete their own account.");
        }

        // Safety Rule (b): Cannot delete last active ADMIN
        if ("ADMIN".equals(existing.getRole()) && "Y".equals(existing.getIsActive())) {
            long activeAdminCount = userRepository.countByRoleAndIsActive("ADMIN", "Y");
            if (activeAdminCount <= 1) {
                throw new BusinessRuleException("LAST_ADMIN", "Cannot delete the last active ADMIN user.");
            }
        }

        userRepository.delete(existing);
        log.info("Admin deleted user {}", existing.getUsername());
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
