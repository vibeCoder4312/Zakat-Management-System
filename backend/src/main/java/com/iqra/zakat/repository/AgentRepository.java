package com.iqra.zakat.repository;

import com.iqra.zakat.entity.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Agent> findByIsActiveOrderByNameAsc(String isActive);

    @Query("SELECT a FROM Agent a WHERE (:q IS NULL OR :q = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.contactNumber) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Agent> searchAgents(@Param("q") String q, Pageable pageable);
}
