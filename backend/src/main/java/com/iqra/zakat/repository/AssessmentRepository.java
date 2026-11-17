package com.iqra.zakat.repository;

import com.iqra.zakat.entity.Assessment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    Optional<Assessment> findByZakatYear(Integer zakatYear);
    boolean existsByZakatYear(Integer zakatYear);
    List<Assessment> findAllByOrderByZakatYearDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Assessment a WHERE a.assessmentId = :id")
    Optional<Assessment> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT a.zakatYear FROM Assessment a ORDER BY a.zakatYear DESC")
    List<Integer> findAllDistinctYears();

    @Query("SELECT a FROM Assessment a WHERE (:q IS NULL OR :q = '' OR STR(a.zakatYear) LIKE CONCAT('%', :q, '%') OR LOWER(a.remarks) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Assessment> searchAssessments(@Param("q") String q, Pageable pageable);
}
