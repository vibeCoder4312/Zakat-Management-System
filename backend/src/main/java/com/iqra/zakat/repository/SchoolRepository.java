package com.iqra.zakat.repository;

import com.iqra.zakat.entity.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<School> findByIsActiveOrderByNameAsc(String isActive);

    @Query("SELECT s FROM School s WHERE (:q IS NULL OR :q = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.location) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<School> searchSchools(@Param("q") String q, Pageable pageable);

    @Query(value = "SELECT s.school_id, s.name, s.location, s.is_active, s.created_by, s.created_on, s.updated_by, s.updated_on, " +
                   "COUNT(a.allocation_id) AS beneficiary_count " +
                   "FROM zk_school s " +
                   "LEFT JOIN zk_allocation a ON a.school_id = s.school_id AND a.status <> 'CANCELLED' " +
                   "WHERE (:q IS NULL OR :q = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.location) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                   "GROUP BY s.school_id, s.name, s.location, s.is_active, s.created_by, s.created_on, s.updated_by, s.updated_on",
           countQuery = "SELECT COUNT(s.school_id) FROM zk_school s WHERE (:q IS NULL OR :q = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.location) LIKE LOWER(CONCAT('%', :q, '%')))",
           nativeQuery = true)
    Page<Object[]> searchSchoolsWithCounts(@Param("q") String q, Pageable pageable);
}
