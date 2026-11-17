package com.iqra.zakat.repository;

import com.iqra.zakat.entity.Beneficiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    Optional<Beneficiary> findByBeneficiaryCode(String code);

    @Query(value = "SELECT 'BEN-' || LPAD(nextval('zk_beneficiary_code_seq')::text, 4, '0')", nativeQuery = true)
    String getNextBeneficiaryCode();

    @Query("SELECT b FROM Beneficiary b WHERE " +
           "(:category IS NULL OR :category = '' OR b.beneficiaryCategory = :category) AND " +
           "(:year IS NULL OR b.zakatYear = :year) AND " +
           "(:verificationStatus IS NULL OR :verificationStatus = '' OR b.verificationStatus = :verificationStatus) AND " +
           "(:isActive IS NULL OR :isActive = '' OR b.isActive = :isActive) AND " +
           "(:q IS NULL OR :q = '' OR " +
           " LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(b.beneficiaryCode) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(b.contactNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(b.address) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(b.referredBy) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Beneficiary> searchBeneficiaries(
            @Param("category") String category,
            @Param("year") Integer year,
            @Param("verificationStatus") String verificationStatus,
            @Param("isActive") String isActive,
            @Param("q") String q,
            Pageable pageable);

    @Query("SELECT b FROM Beneficiary b WHERE b.verificationStatus = 'VERIFIED' AND b.isActive = 'Y' AND " +
           "(:q IS NULL OR :q = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(b.beneficiaryCode) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY b.name ASC")
    List<Beneficiary> findVerifiedActiveTypeahead(@Param("q") String q, Pageable pageable);

    @Query("SELECT b.beneficiaryCategory, COUNT(b) FROM Beneficiary b GROUP BY b.beneficiaryCategory")
    List<Object[]> countByCategory();

    long countByVerificationStatusAndIsActive(String verificationStatus, String isActive);

    @Modifying
    @Query("UPDATE Beneficiary b SET b.beneficiaryCategory = :newName WHERE LOWER(b.beneficiaryCategory) = LOWER(:oldName)")
    void updateCategoryName(@Param("oldName") String oldName, @Param("newName") String newName);
}
