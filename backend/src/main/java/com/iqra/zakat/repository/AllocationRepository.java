package com.iqra.zakat.repository;

import com.iqra.zakat.entity.Allocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    @Query("SELECT a.schoolId, COUNT(a) FROM Allocation a WHERE a.schoolId IS NOT NULL GROUP BY a.schoolId")
    List<Object[]> countBySchool();

    boolean existsByAssessmentId(Long assessmentId);
    boolean existsByClassificationId(Long classificationId);
    boolean existsByBeneficiaryId(Long beneficiaryId);
    boolean existsBySchoolId(Long schoolId);
    boolean existsByPaidSourceAccountId(Long sourceAccountId);
    boolean existsByPlannedSourceAccountId(Long sourceAccountId);
    boolean existsByPaidViaAgentId(Long agentId);
    boolean existsByPaidViaEmployeeIgnoreCase(String employeeName);

    @Query("SELECT a FROM Allocation a WHERE " +
           "(:years IS NULL OR a.zakatYear IN :years) AND " +
           "(:classificationId IS NULL OR a.classificationId = :classificationId) AND " +
           "(:category IS NULL OR :category = '' OR a.category = :category) AND " +
           "(:status IS NULL OR :status = '' OR a.status = :status) AND " +
           "(:approvalStatus IS NULL OR :approvalStatus = '' OR a.approvalStatus = :approvalStatus) AND " +
           "(:beneficiaryId IS NULL OR a.beneficiaryId = :beneficiaryId) AND " +
           "(:sourceAccountId IS NULL OR a.paidSourceAccountId = :sourceAccountId) AND " +
           "(:schoolId IS NULL OR a.schoolId = :schoolId) AND " +
           "(:paymentMethod IS NULL OR :paymentMethod = '' OR a.paymentMethod = :paymentMethod) AND " +
           "(:q IS NULL OR :q = '' OR " +
           " LOWER(a.beneficiary) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.classification) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.category) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.remarks) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.school) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.paidSourceAccount) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Allocation> searchAllocations(
            @Param("years") List<Integer> years,
            @Param("classificationId") Long classificationId,
            @Param("category") String category,
            @Param("status") String status,
            @Param("approvalStatus") String approvalStatus,
            @Param("beneficiaryId") Long beneficiaryId,
            @Param("sourceAccountId") Long sourceAccountId,
            @Param("schoolId") Long schoolId,
            @Param("paymentMethod") String paymentMethod,
            @Param("q") String q,
            Pageable pageable);

    @Query("SELECT COUNT(a), " +
           "COALESCE(SUM(a.plannedAmount), 0), " +
           "COALESCE(SUM(a.paidAmount), 0), " +
           "COALESCE(SUM(CASE WHEN a.status <> 'CANCELLED' THEN (CASE WHEN a.plannedAmount > a.paidAmount THEN a.plannedAmount - a.paidAmount ELSE 0 END) ELSE 0 END), 0) " +
           "FROM Allocation a WHERE " +
           "(:years IS NULL OR a.zakatYear IN :years) AND " +
           "(:classificationId IS NULL OR a.classificationId = :classificationId) AND " +
           "(:category IS NULL OR :category = '' OR a.category = :category) AND " +
           "(:status IS NULL OR :status = '' OR a.status = :status) AND " +
           "(:approvalStatus IS NULL OR :approvalStatus = '' OR a.approvalStatus = :approvalStatus) AND " +
           "(:beneficiaryId IS NULL OR a.beneficiaryId = :beneficiaryId) AND " +
           "(:sourceAccountId IS NULL OR a.paidSourceAccountId = :sourceAccountId) AND " +
           "(:schoolId IS NULL OR a.schoolId = :schoolId) AND " +
           "(:paymentMethod IS NULL OR :paymentMethod = '' OR a.paymentMethod = :paymentMethod) AND " +
           "(:q IS NULL OR :q = '' OR " +
           " LOWER(a.beneficiary) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.classification) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.category) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.remarks) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.school) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(a.paidSourceAccount) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Object[]> getFilteredSummary(
            @Param("years") List<Integer> years,
            @Param("classificationId") Long classificationId,
            @Param("category") String category,
            @Param("status") String status,
            @Param("approvalStatus") String approvalStatus,
            @Param("beneficiaryId") Long beneficiaryId,
            @Param("sourceAccountId") Long sourceAccountId,
            @Param("schoolId") Long schoolId,
            @Param("paymentMethod") String paymentMethod,
            @Param("q") String q);

    @Query("SELECT a FROM Allocation a WHERE a.zakatYear = :year AND a.classificationId = :classificationId AND LOWER(TRIM(a.category)) = LOWER(TRIM(:category)) AND a.status <> 'CANCELLED'")
    List<Allocation> findMatchingForPlanning(
            @Param("year") Integer year,
            @Param("classificationId") Long classificationId,
            @Param("category") String category);

    @Query(value = "SELECT c.name AS classification, a.zakat_year, SUM(a.paid_amount) AS paid_amt " +
                   "FROM zk_allocation a " +
                   "JOIN zk_classification c ON c.classification_id = a.classification_id " +
                   "WHERE a.status = 'PAID' " +
                   "GROUP BY c.name, a.zakat_year " +
                   "ORDER BY c.name, a.zakat_year", nativeQuery = true)
    List<Object[]> getClassificationByYearPivotData();

    @Query(value = "SELECT a.category, a.zakat_year, SUM(a.paid_amount) AS paid_amt " +
                   "FROM zk_allocation a " +
                   "WHERE a.status = 'PAID' AND a.category IS NOT NULL AND a.category <> '' " +
                   "GROUP BY a.category, a.zakat_year " +
                   "ORDER BY a.category, a.zakat_year", nativeQuery = true)
    List<Object[]> getCategoryByYearPivotData();

    /**
     * ZAKAT-fund committed amount for an assessment (same rule as fn_recalc_assessment):
     * PAID rows count their paid amount, PLANNED rows their planned amount, CANCELLED rows nothing.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN a.status = 'PAID' THEN a.paidAmount " +
           "WHEN a.status = 'PLANNED' THEN a.plannedAmount ELSE 0 END), 0) " +
           "FROM Allocation a JOIN a.classificationEntity c " +
           "WHERE a.assessmentId = :assessmentId AND c.fundType = 'ZAKAT' AND a.status <> 'CANCELLED'")
    BigDecimal sumZakatCommitted(@Param("assessmentId") Long assessmentId);

    // Dynamic batch query for dashboard: active allocations in selected years with eager-fetched classification
    @Query("SELECT a FROM Allocation a LEFT JOIN FETCH a.classificationEntity WHERE (:years IS NULL OR a.zakatYear IN :years) AND a.status <> 'CANCELLED'")
    List<Allocation> findActiveByYearsWithClassification(@Param("years") List<Integer> years);

    // Dynamic pre-fetch query for planning historical 5-year paid sums
    @Query("SELECT a.zakatYear, a.classificationId, LOWER(TRIM(a.category)), COALESCE(SUM(a.paidAmount), 0) " +
           "FROM Allocation a WHERE a.zakatYear BETWEEN :minYear AND :maxYear " +
           "AND a.status <> 'CANCELLED' AND a.category IS NOT NULL " +
           "GROUP BY a.zakatYear, a.classificationId, LOWER(TRIM(a.category))")
    List<Object[]> sumPaidByYearClassificationAndCategory(@Param("minYear") Integer minYear, @Param("maxYear") Integer maxYear);

    // Modifying update queries to propagate renames to denormalized columns on zk_allocation
    @Modifying
    @Query("UPDATE Allocation a SET a.classification = :newName WHERE a.classificationId = :id")
    void updateClassificationName(@Param("id") Long id, @Param("newName") String newName);

    @Modifying
    @Query("UPDATE Allocation a SET a.beneficiary = :newName WHERE a.beneficiaryId = :id")
    void updateBeneficiaryName(@Param("id") Long id, @Param("newName") String newName);

    @Modifying
    @Query("UPDATE Allocation a SET a.school = :newName WHERE a.schoolId = :id")
    void updateSchoolName(@Param("id") Long id, @Param("newName") String newName);

    @Modifying
    @Query("UPDATE Allocation a SET a.paidSourceAccount = :newName WHERE a.paidSourceAccountId = :id")
    void updatePaidSourceAccountName(@Param("id") Long id, @Param("newName") String newName);

    @Modifying
    @Query("UPDATE Allocation a SET a.paidViaAgent = :newName WHERE a.paidViaAgentId = :id")
    void updatePaidViaAgentName(@Param("id") Long id, @Param("newName") String newName);

    @Modifying
    @Query("UPDATE Allocation a SET a.paidViaEmployee = :newName WHERE LOWER(a.paidViaEmployee) = LOWER(:oldName)")
    void updatePaidViaEmployeeName(@Param("oldName") String oldName, @Param("newName") String newName);

    @Modifying
    @Query("UPDATE Allocation a SET a.category = :newName WHERE LOWER(a.category) = LOWER(:oldName)")
    void updateCategoryName(@Param("oldName") String oldName, @Param("newName") String newName);

    // Native queries for isolated receipt file storage
    @Modifying
    @Query(value = "UPDATE zk_allocation SET receipt_file = :fileBytes, receipt_filename = :filename, receipt_mimetype = :mimetype, receipt_received = 'Y', updated_by = :updatedBy, updated_on = CURRENT_TIMESTAMP WHERE allocation_id = :id", nativeQuery = true)
    void updateReceiptFile(
            @Param("id") Long id,
            @Param("fileBytes") byte[] fileBytes,
            @Param("filename") String filename,
            @Param("mimetype") String mimetype,
            @Param("updatedBy") String updatedBy);

    @Query(value = "SELECT receipt_file FROM zk_allocation WHERE allocation_id = :id", nativeQuery = true)
    byte[] findReceiptBytesById(@Param("id") Long id);
}
