package com.iqra.zakat.repository;

import com.iqra.zakat.entity.Classification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassificationRepository extends JpaRepository<Classification, Long> {
    Optional<Classification> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Classification> findByIsActiveOrderByDisplayOrderAscNameAsc(String isActive);

    @Query("SELECT c FROM Classification c WHERE (:q IS NULL OR :q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Classification> searchClassifications(@Param("q") String q, Pageable pageable);
}
