package com.iqra.zakat.entity;

import com.iqra.zakat.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zk_source_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceAccount extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_account_id")
    private Long sourceAccountId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "region", nullable = false, length = 10)
    @Builder.Default
    private String region = "INDIA"; // INDIA, DUBAI

    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";
}
