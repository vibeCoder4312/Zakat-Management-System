package com.iqra.zakat.controller;

import com.iqra.zakat.dto.AuthDto;
import com.iqra.zakat.dto.PagedResponse;
import com.iqra.zakat.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/users", "/api/users"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Administration", description = "Admin-only user and role management")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List and search application users with pagination")
    public ResponseEntity<PagedResponse<AuthDto.UserDto>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(userService.listUsers(page, size, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<AuthDto.UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping
    @Operation(summary = "Create a new application user")
    public ResponseEntity<AuthDto.UserDto> createUser(@Valid @RequestBody AuthDto.CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user's details or reset password")
    public ResponseEntity<AuthDto.UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AuthDto.UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }
}
