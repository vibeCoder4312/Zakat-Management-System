package com.iqra.zakat.entity;

import com.iqra.zakat.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "zk_planning")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Planning extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planning_id")
    private Long planningId;

    @Column(name = "zakat_year", nullable = false)
    private Integer zakatYear;

    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType; // ZAKAT, SADAQAH

    @Column(name = "category", length = 250)
    private String category;

    @Column(name = "classification_id")
    private Long classificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id", insertable = false, updatable = false)
    private Classification classificationEntity;

    @Column(name = "planned_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal plannedAmount = BigDecimal.ZERO;

    @Column(name = "remarks", length = 1000)
    private String remarks;
}
