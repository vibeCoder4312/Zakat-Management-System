package com.iqra.zakat.entity;

import com.iqra.zakat.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "zk_assessment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assessment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_id")
    private Long assessmentId;

    @Column(name = "zakat_year", nullable = false, unique = true)
    private Integer zakatYear;

    @Column(name = "zakat_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal zakatAmount = BigDecimal.ZERO;

    @Column(name = "carry_forward", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal carryForward = BigDecimal.ZERO;

    @Column(name = "net_payable", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal netPayable = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private String status = "OPEN"; // OPEN, CLOSED

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "total_allocated_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalAllocatedAmount = BigDecimal.ZERO;

    @Column(name = "remaining_balance", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal remainingBalance = BigDecimal.ZERO;
}
