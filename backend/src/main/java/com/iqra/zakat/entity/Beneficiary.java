package com.iqra.zakat.entity;

import com.iqra.zakat.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "zk_beneficiary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "beneficiary_id")
    private Long beneficiaryId;

    @Column(name = "beneficiary_code", unique = true, length = 20)
    private String beneficiaryCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "referred_by", length = 100)
    private String referredBy;

    @Column(name = "verification_status", nullable = false, length = 10)
    @Builder.Default
    private String verificationStatus = "PENDING"; // PENDING, VERIFIED, CANCELLED

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_on")
    private OffsetDateTime verifiedOn;

    @Column(name = "verification_note", length = 500)
    private String verificationNote;

    @Column(name = "is_active", nullable = false, length = 1)
    @Builder.Default
    private String isActive = "Y";

    @Column(name = "beneficiary_category", length = 100)
    private String beneficiaryCategory;

    @Column(name = "zakat_year")
    private Integer zakatYear;
}
