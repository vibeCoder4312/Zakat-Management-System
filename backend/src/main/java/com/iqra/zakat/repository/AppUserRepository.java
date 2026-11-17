package com.iqra.zakat.repository;

import com.iqra.zakat.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    long countByRoleAndIsActive(String role, String isActive);

    @Query("SELECT u FROM AppUser u WHERE (:q IS NULL OR :q = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.role) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<AppUser> searchUsers(@Param("q") String q, Pageable pageable);
}
