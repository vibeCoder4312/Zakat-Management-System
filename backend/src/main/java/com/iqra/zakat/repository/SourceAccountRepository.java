package com.iqra.zakat.repository;

import com.iqra.zakat.entity.SourceAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SourceAccountRepository extends JpaRepository<SourceAccount, Long> {
    Optional<SourceAccount> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<SourceAccount> findByIsActiveOrderByNameAsc(String isActive);

    @Query("SELECT s FROM SourceAccount s WHERE (:q IS NULL OR :q = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.region) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<SourceAccount> searchSourceAccounts(@Param("q") String q, Pageable pageable);
}
