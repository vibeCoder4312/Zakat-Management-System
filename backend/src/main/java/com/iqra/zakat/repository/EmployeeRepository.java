package com.iqra.zakat.repository;

import com.iqra.zakat.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Employee> findByIsActiveOrderByNameAsc(String isActive);

    @Query("SELECT e FROM Employee e WHERE (:q IS NULL OR :q = '' OR LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.remarks) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Employee> searchEmployees(@Param("q") String q, Pageable pageable);
}
