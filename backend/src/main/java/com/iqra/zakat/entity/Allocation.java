package com.iqra.zakat.entity;

import com.iqra.zakat.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "zk_allocation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Allocation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Long allocationId;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
    private Assessment assessment;

    @Column(name = "classification_id", nullable = false)
    private Long classificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", insertable = false, updatable = false)
    private Classification classificationEntity;

    @Column(name = "beneficiary_id", nullable = false)
    private Long beneficiaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", insertable = false, updatable = false)
    private Beneficiary beneficiaryEntity;

    @Column(name = "planned_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal plannedAmount = BigDecimal.ZERO;

    @Column(name = "planned_source_account_id")
    private Long plannedSourceAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planned_source_account_id", insertable = false, updatable = false)
    private SourceAccount plannedSourceAccount;

    @Column(name = "school_id")
    private Long schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", insertable = false, updatable = false)
    private School schoolEntity;

    @Column(name = "current_std", length = 30)
    private String currentStd;

    @Column(name = "prev_std_result", length = 250)
    private String prevStdResult;

    @Column(name = "paid_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    @Column(name = "payment_method", length = 10)
    private String paymentMethod; // CASH, ONLINE, BANK

    @Column(name = "paid_source_account_id")
    private Long paidSourceAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_source_account_id", insertable = false, updatable = false)
    private SourceAccount paidSourceAccountEntity;

    @Column(name = "paid_via_agent_id")
    private Long paidViaAgentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_via_agent_id", insertable = false, updatable = false)
    private Agent paidViaAgentEntity;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "INR"; // INR, AED

    @Column(name = "foreign_amount", precision = 18, scale = 2)
    private BigDecimal foreignAmount;

    @Column(name = "fx_rate", precision = 10, scale = 4)
    private BigDecimal fxRate;

    @Column(name = "receipt_received", nullable = false, length = 1)
    @Builder.Default
    private String receiptReceived = "N"; // Y, N

    @Column(name = "receipt_filename", length = 255)
    private String receiptFilename;

    @Column(name = "receipt_mimetype", length = 100)
    private String receiptMimetype;

    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private String status = "PLANNED"; // PLANNED, PAID, CANCELLED

    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private String approvalStatus = "PENDING"; // PENDING, APPROVED

    @Column(name = "category", length = 250)
    private String category;

    @Column(name = "zakat_year", nullable = false)
    private Integer zakatYear;

    @Column(name = "remarks", length = 500)
    private String remarks;

    // Denormalized text columns preserved from Oracle
    @Column(name = "classification", length = 100)
    private String classification;

    @Column(name = "beneficiary", length = 200)
    private String beneficiary;

    @Column(name = "school", length = 200)
    private String school;

    @Column(name = "paid_source_account", length = 200)
    private String paidSourceAccount;

    @Column(name = "paid_via_agent", length = 200)
    private String paidViaAgent;

    @Column(name = "paid_via_employee", length = 100)
    private String paidViaEmployee;
}
