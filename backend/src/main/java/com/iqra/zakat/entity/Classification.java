package com.iqra.zakat.entity;

import com.iqra.zakat.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zk_classification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classification extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "classification_id")
    private Long classificationId;

    @Column(name = "code", length = 30, unique = true)
    private String code;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "fund_type", nullable = false, length = 10)
    private String fundType; // ZAKAT, SADAQAH

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 99;

    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";
}
