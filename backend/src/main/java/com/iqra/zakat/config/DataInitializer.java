package com.iqra.zakat.config;

import com.iqra.zakat.entity.AppUser;
import com.iqra.zakat.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.seed-default-users:true}")
    private boolean seedDefaultUsers;

    @Value("${app.security.initial-admin-password:admin123}")
    private String initialAdminPassword;

    @Value("${app.security.initial-accountant-password:account123}")
    private String initialAccountantPassword;

    @Value("${app.security.initial-viewer-password:viewer123}")
    private String initialViewerPassword;

    @Override
    public void run(String... args) {
        if (!seedDefaultUsers) {
            log.info("Default user seeding is disabled (app.security.seed-default-users=false)");
            return;
        }

        initUserIfMissing("admin", initialAdminPassword, "System Administrator", "ADMIN");
        initUserIfMissing("accountant", initialAccountantPassword, "Accountant User", "ACCOUNTANT");
        initUserIfMissing("viewer", initialViewerPassword, "Read-Only Viewer", "VIEWER");
    }

    private void initUserIfMissing(String username, String rawPassword, String fullName, String role) {
        if (userRepository.existsByUsername(username)) {
            log.debug("User {} already exists. Skipping initialization to preserve password and role.", username);
            return;
        }

        AppUser user = AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role(role)
                .isActive("Y")
                .build();
        user.setCreatedBy("SYSTEM");
        user.setCreatedOn(OffsetDateTime.now());
        user.setUpdatedBy("SYSTEM");
        user.setUpdatedOn(OffsetDateTime.now());
        userRepository.save(user);
        log.info("Initialized default user: {} with role: {}", username, role);
    }
}
