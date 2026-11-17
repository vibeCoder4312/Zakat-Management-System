package com.iqra.zakat.service;

import com.iqra.zakat.dto.BeneficiaryDto;
import com.iqra.zakat.entity.Beneficiary;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.mapper.EntityMappers;
import com.iqra.zakat.repository.AllocationRepository;
import com.iqra.zakat.repository.BeneficiaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private AllocationRepository allocationRepository;

    @Mock
    private AuthService authService;

    @Spy
    private EntityMappers mapper = new EntityMappers();

    @InjectMocks
    private BeneficiaryService beneficiaryService;

    @Test
    @DisplayName("Rule 8: Automatically generate beneficiary code when empty")
    void testBeneficiaryCodeGenerated() {
        BeneficiaryDto.Request req = BeneficiaryDto.Request.builder()
                .name("New Beneficiary")
                .beneficiaryCategory("Poor Family")
                .build();

        when(beneficiaryRepository.getNextBeneficiaryCode()).thenReturn("BEN-0969");
        when(authService.getCurrentUsername()).thenReturn("admin");
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(i -> {
            Beneficiary b = i.getArgument(0);
            b.setBeneficiaryId(969L);
            return b;
        });

        BeneficiaryDto.Response resp = beneficiaryService.create(req);

        assertNotNull(resp);
        assertEquals("BEN-0969", resp.getBeneficiaryCode());
        assertEquals("PENDING", resp.getVerificationStatus());
    }

    @Test
    @DisplayName("Rule 9: Setting status to VERIFIED populates verified_by and verified_on")
    void testVerificationStatusTransition() {
        Beneficiary b = Beneficiary.builder()
                .beneficiaryId(1L)
                .name("Test Beneficiary")
                .verificationStatus("PENDING")
                .build();

        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(b));
        when(authService.getCurrentUsername()).thenReturn("accountant");
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(i -> i.getArgument(0));

        BeneficiaryDto.VerificationRequest req = new BeneficiaryDto.VerificationRequest("VERIFIED", "Verified address and income");
        BeneficiaryDto.Response resp = beneficiaryService.updateVerification(1L, req);

        assertEquals("VERIFIED", resp.getVerificationStatus());
        assertEquals("accountant", resp.getVerifiedBy());
        assertNotNull(resp.getVerifiedOn());
    }

    @Test
    @DisplayName("Rule 10: Changing away from VERIFIED clears verification audit fields")
    void testUnverifyClearsAudit() {
        Beneficiary b = Beneficiary.builder()
                .beneficiaryId(1L)
                .name("Test Beneficiary")
                .verificationStatus("VERIFIED")
                .verifiedBy("admin")
                .build();

        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(b));
        when(authService.getCurrentUsername()).thenReturn("admin");
        when(beneficiaryRepository.save(any(Beneficiary.class))).thenAnswer(i -> i.getArgument(0));

        BeneficiaryDto.VerificationRequest req = new BeneficiaryDto.VerificationRequest("CANCELLED", "Cancelled beneficiary");
        BeneficiaryDto.Response resp = beneficiaryService.updateVerification(1L, req);

        assertEquals("CANCELLED", resp.getVerificationStatus());
        assertNull(resp.getVerifiedBy());
        assertNull(resp.getVerifiedOn());
    }

    @Test
    @DisplayName("Rule 30: Cannot delete beneficiary with existing allocations")
    void testDeleteBeneficiaryWithAllocationsFails() {
        Beneficiary b = Beneficiary.builder().beneficiaryId(1L).name("Active Ben").build();
        when(beneficiaryRepository.findById(1L)).thenReturn(Optional.of(b));
        when(allocationRepository.existsByBeneficiaryId(1L)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                beneficiaryService.delete(1L)
        );

        assertTrue(ex.getMessage().contains("Beneficiary has existing allocations and cannot be deleted"));
    }
}
