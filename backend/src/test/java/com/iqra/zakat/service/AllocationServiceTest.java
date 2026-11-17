package com.iqra.zakat.service;

import com.iqra.zakat.dto.AllocationDto;
import com.iqra.zakat.entity.Allocation;
import com.iqra.zakat.entity.Assessment;
import com.iqra.zakat.entity.Beneficiary;
import com.iqra.zakat.entity.Classification;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.mapper.EntityMappers;
import com.iqra.zakat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock
    private AllocationRepository allocationRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private ClassificationRepository classificationRepository;
    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private SourceAccountRepository sourceAccountRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AssessmentService assessmentService;
    @Mock
    private AuthService authService;

    @Spy
    private EntityMappers mapper = new EntityMappers();

    @InjectMocks
    private AllocationService allocationService;

    private Assessment openAssessment;
    private Assessment closedAssessment;
    private Beneficiary verifiedActiveBen;
    private Beneficiary pendingBen;
    private Classification zakatCls;

    @BeforeEach
    void setUp() {
        openAssessment = Assessment.builder()
                .assessmentId(1L)
                .zakatYear(2026)
                .status("OPEN")
                .zakatAmount(new BigDecimal("100000"))
                .netPayable(new BigDecimal("100000"))
                .build();

        closedAssessment = Assessment.builder()
                .assessmentId(2L)
                .zakatYear(2024)
                .status("CLOSED")
                .zakatAmount(new BigDecimal("100000"))
                .netPayable(new BigDecimal("100000"))
                .build();

        verifiedActiveBen = Beneficiary.builder()
                .beneficiaryId(10L)
                .name("Verified Beneficiary")
                .verificationStatus("VERIFIED")
                .isActive("Y")
                .build();

        pendingBen = Beneficiary.builder()
                .beneficiaryId(11L)
                .name("Pending Beneficiary")
                .verificationStatus("PENDING")
                .isActive("Y")
                .build();

        zakatCls = Classification.builder()
                .classificationId(5L)
                .name("Poor Family")
                .fundType("ZAKAT")
                .build();
    }

    @Test
    @DisplayName("Rule 6: Reject allocation creation in CLOSED year")
    void testClosedYearAllocationRejected() {
        when(assessmentRepository.findByZakatYear(2024)).thenReturn(Optional.of(closedAssessment));

        AllocationDto.Request req = AllocationDto.Request.builder()
                .zakatYear(2024)
                .beneficiaryId(10L)
                .classificationId(5L)
                .plannedAmount(new BigDecimal("5000"))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                allocationService.create(req)
        );

        assertTrue(ex.getMessage().contains("This year is CLOSED. Re-open the Zakat year to change allocations or payments."));
    }

    @Test
    @DisplayName("Rule 15: Reject allocation if beneficiary is NOT verified")
    void testNonVerifiedBeneficiaryRejected() {
        when(assessmentRepository.findByZakatYear(2026)).thenReturn(Optional.of(openAssessment));
        when(beneficiaryRepository.findById(11L)).thenReturn(Optional.of(pendingBen));

        AllocationDto.Request req = AllocationDto.Request.builder()
                .zakatYear(2026)
                .beneficiaryId(11L)
                .classificationId(5L)
                .plannedAmount(new BigDecimal("5000"))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                allocationService.create(req)
        );

        assertTrue(ex.getMessage().contains("Beneficiary must be Verified and Active before an allocation can be made."));
    }

    @Test
    @DisplayName("Rule 16: Foreign currency conversion (AED to INR)")
    void testForeignCurrencyConversion() {
        when(assessmentRepository.findByZakatYear(2026)).thenReturn(Optional.of(openAssessment));
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(verifiedActiveBen));
        when(classificationRepository.findById(5L)).thenReturn(Optional.of(zakatCls));
        when(authService.getCurrentUsername()).thenReturn("admin");
        when(allocationRepository.save(any(Allocation.class))).thenAnswer(i -> {
            Allocation a = i.getArgument(0);
            a.setAllocationId(100L);
            return a;
        });

        AllocationDto.Request req = AllocationDto.Request.builder()
                .zakatYear(2026)
                .beneficiaryId(10L)
                .classificationId(5L)
                .plannedAmount(new BigDecimal("100000"))
                .currencyCode("AED")
                .foreignAmount(new BigDecimal("1000.00"))
                .fxRate(new BigDecimal("22.5000"))
                .build();

        AllocationDto.Response resp = allocationService.create(req);

        assertNotNull(resp);
        assertEquals(new BigDecimal("22500.00"), resp.getPaidAmount(), "Paid amount should be foreignAmount * fxRate");
        assertEquals("PAID", resp.getStatus(), "Status should be PAID because paid amount > 0");
        assertEquals("APPROVED", resp.getApprovalStatus(), "Approval status should be APPROVED when paid > 0");
    }

    @Test
    @DisplayName("Rule 18: Cancellation rejected if payment is recorded")
    void testCancellationWithPaymentRejected() {
        Allocation existing = Allocation.builder()
                .allocationId(50L)
                .assessmentId(1L)
                .zakatYear(2026)
                .paidAmount(new BigDecimal("5000"))
                .status("PAID")
                .build();

        when(allocationRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(openAssessment));

        AllocationDto.Request req = AllocationDto.Request.builder()
                .zakatYear(2026)
                .plannedAmount(new BigDecimal("5000"))
                .paidAmount(new BigDecimal("5000")) // has payment
                .status("CANCELLED")
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                allocationService.update(50L, req)
        );

        assertTrue(ex.getMessage().contains("A payment is recorded on this allocation. Clear the paid amount before cancelling."));
    }

    @Test
    @DisplayName("Partial payments: Outstanding amount is calculated accurately")
    void testPartialPaymentOutstanding() {
        Allocation a = Allocation.builder()
                .allocationId(1L)
                .plannedAmount(new BigDecimal("10000.00"))
                .paidAmount(new BigDecimal("4000.00"))
                .status("PAID")
                .build();

        AllocationDto.Response resp = mapper.toAllocationResponse(a);

        assertEquals(new BigDecimal("6000.00"), resp.getOutstandingAmount(), "Outstanding should be 10000 - 4000 = 6000");
    }

    @Test
    @DisplayName("Cross-year allocation update recalculates totals for both previous and new assessments")
    void testCrossYearAllocationUpdateRecalculatesBothAssessments() {
        Assessment assessment2025 = Assessment.builder()
                .assessmentId(3L)
                .zakatYear(2025)
                .status("OPEN")
                .zakatAmount(new BigDecimal("100000"))
                .netPayable(new BigDecimal("100000"))
                .build();

        Allocation existing = Allocation.builder()
                .allocationId(60L)
                .assessmentId(1L) // 2026
                .zakatYear(2026)
                .plannedAmount(new BigDecimal("5000"))
                .paidAmount(BigDecimal.ZERO)
                .status("PLANNED")
                .build();

        when(allocationRepository.findById(60L)).thenReturn(Optional.of(existing));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(openAssessment));
        when(assessmentRepository.findByZakatYear(2025)).thenReturn(Optional.of(assessment2025));
        when(authService.getCurrentUsername()).thenReturn("admin");
        when(allocationRepository.save(any(Allocation.class))).thenAnswer(i -> i.getArgument(0));

        AllocationDto.Request req = AllocationDto.Request.builder()
                .zakatYear(2025)
                .plannedAmount(new BigDecimal("5000"))
                .build();

        AllocationDto.Response resp = allocationService.update(60L, req);

        assertNotNull(resp);
        assertEquals(2025, resp.getZakatYear());
        verify(assessmentService).recalculateTotals(3L);
        verify(assessmentService).recalculateTotals(1L);
    }

    @Test
    @DisplayName("uploadReceipt throws BusinessRuleException when assessment year is CLOSED")
    void testUploadReceiptClosedYearThrows() {
        Allocation existing = Allocation.builder()
                .allocationId(70L)
                .assessmentId(2L) // closed year 2024
                .zakatYear(2024)
                .build();

        when(allocationRepository.findById(70L)).thenReturn(Optional.of(existing));
        when(assessmentRepository.findById(2L)).thenReturn(Optional.of(closedAssessment));

        byte[] bytes = "dummy pdf content".getBytes();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                allocationService.uploadReceipt(70L, bytes, "receipt.pdf", "application/pdf")
        );

        assertTrue(ex.getMessage().contains("Cannot upload receipt for a CLOSED assessment year"));
    }

    @Test
    @DisplayName("uploadReceipt throws BusinessRuleException when invalid MIME type is supplied")
    void testUploadReceiptInvalidMimeTypeThrows() {
        Allocation existing = Allocation.builder()
                .allocationId(71L)
                .assessmentId(1L) // open year 2026
                .zakatYear(2026)
                .build();

        when(allocationRepository.findById(71L)).thenReturn(Optional.of(existing));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(openAssessment));

        byte[] bytes = new byte[]{0x4D, 0x5A}; // exe bytes
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                allocationService.uploadReceipt(71L, bytes, "file.exe", "application/x-msdownload")
        );

        assertTrue(ex.getMessage().contains("Invalid receipt file type"));
    }

    @Test
    @DisplayName("uploadReceipt throws BusinessRuleException when file is empty or exceeds 10MB")
    void testUploadReceiptSizeValidation() {
        Allocation existing = Allocation.builder()
                .allocationId(72L)
                .assessmentId(1L)
                .zakatYear(2026)
                .build();

        when(allocationRepository.findById(72L)).thenReturn(Optional.of(existing));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(openAssessment));

        assertThrows(BusinessRuleException.class, () ->
                allocationService.uploadReceipt(72L, new byte[0], "empty.pdf", "application/pdf")
        );
    }
}
