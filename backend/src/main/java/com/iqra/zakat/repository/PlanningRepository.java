package com.iqra.zakat.repository;

import com.iqra.zakat.entity.Planning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanningRepository extends JpaRepository<Planning, Long> {
    List<Planning> findByZakatYearOrderByPlanningIdAsc(Integer zakatYear);

    @Query("SELECT p FROM Planning p LEFT JOIN FETCH p.classificationEntity WHERE (:years IS NULL OR p.zakatYear IN :years) ORDER BY p.zakatYear DESC, p.planningId ASC")
    List<Planning> findByYears(@Param("years") List<Integer> years);

    boolean existsByClassificationId(Long classificationId);

    boolean existsByCategoryIgnoreCase(String category);

    @Modifying
    @Query("UPDATE Planning p SET p.category = :newName WHERE LOWER(p.category) = LOWER(:oldName)")
    void updateCategoryName(@Param("oldName") String oldName, @Param("newName") String newName);
}
