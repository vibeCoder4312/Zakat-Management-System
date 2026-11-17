package com.iqra.zakat.entity;

import com.iqra.zakat.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "zk_school")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";
}
