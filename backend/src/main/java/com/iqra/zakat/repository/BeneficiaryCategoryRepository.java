package com.iqra.zakat.repository;

import com.iqra.zakat.entity.BeneficiaryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryCategoryRepository extends JpaRepository<BeneficiaryCategory, Long> {
    Optional<BeneficiaryCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<BeneficiaryCategory> findByIsActiveOrderByNameAsc(String isActive);

    @Query("SELECT c FROM BeneficiaryCategory c WHERE (:q IS NULL OR :q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<BeneficiaryCategory> searchCategories(@Param("q") String q, Pageable pageable);

    @Query(value = "SELECT c.category_id, c.name, c.is_active, c.created_by, c.created_on, c.updated_by, c.updated_on, " +
                   "COUNT(b.beneficiary_id) AS beneficiary_count " +
                   "FROM zk_beneficiary_category c " +
                   "LEFT JOIN zk_beneficiary b ON LOWER(b.beneficiary_category) = LOWER(c.name) " +
                   "WHERE (:q IS NULL OR :q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))) " +
                   "GROUP BY c.category_id, c.name, c.is_active, c.created_by, c.created_on, c.updated_by, c.updated_on",
           countQuery = "SELECT COUNT(c.category_id) FROM zk_beneficiary_category c WHERE (:q IS NULL OR :q = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))",
           nativeQuery = true)
    Page<Object[]> searchCategoriesWithCounts(@Param("q") String q, Pageable pageable);
}
