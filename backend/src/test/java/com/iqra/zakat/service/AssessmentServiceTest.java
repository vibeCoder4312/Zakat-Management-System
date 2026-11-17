package com.iqra.zakat.service;

import com.iqra.zakat.dto.AssessmentDto;
import com.iqra.zakat.entity.Assessment;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.mapper.EntityMappers;
import com.iqra.zakat.repository.AllocationRepository;
import com.iqra.zakat.repository.AssessmentRepository;
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
class AssessmentServiceTest {

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private AllocationRepository allocationRepository;

    @Mock
    private AuthService authService;

    @Spy
    private EntityMappers mapper = new EntityMappers();

    @InjectMocks
    private AssessmentService assessmentService;

    private Assessment openAssessment;
    private Assessment closedAssessment;

    @BeforeEach
    void setUp() {
        openAssessment = Assessment.builder()
                .assessmentId(1L)
                .zakatYear(2026)
                .zakatAmount(new BigDecimal("3944546.38"))
                .carryForward(BigDecimal.ZERO)
                .netPayable(new BigDecimal("3944546.38"))
                .totalAllocatedAmount(new BigDecimal("3945212.00"))
                .remainingBalance(new BigDecimal("-665.62"))
                .status("OPEN")
                .remarks("2026 Assessment")
                .build();

        closedAssessment = Assessment.builder()
                .assessmentId(2L)
                .zakatYear(2024)
                .zakatAmount(new BigDecimal("1795550.00"))
                .carryForward(BigDecimal.ZERO)
                .netPayable(new BigDecimal("1795550.00"))
                .totalAllocatedAmount(new BigDecimal("1795550.00"))
                .remainingBalance(BigDecimal.ZERO)
                .status("CLOSED")
                .remarks("2024 Closed Assessment")
                .build();
    }

    @Test
    @DisplayName("Rule 1 & 2: net_payable and remaining_balance calculations")
    void testAssessmentCalculations() {
        AssessmentDto.Request req = AssessmentDto.Request.builder()
                .zakatYear(2027)
                .zakatAmount(new BigDecimal("100000.00"))
                .carryForward(new BigDecimal("10000.00"))
                .status("OPEN")
                .build();

        when(assessmentRepository.existsByZakatYear(2027)).thenReturn(false);
        when(authService.getCurrentUsername()).thenReturn("admin");
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(i -> {
            Assessment a = i.getArgument(0);
            a.setAssessmentId(3L);
            return a;
        });

        AssessmentDto.Response resp = assessmentService.createAssessment(req);

        assertNotNull(resp);
        assertEquals(new BigDecimal("90000.00"), resp.getNetPayable(), "Net payable should be zakat - carry_forward");
        assertEquals(new BigDecimal("90000.00"), resp.getRemainingBalance(), "Initial remaining balance equals net payable");
    }

    @Test
    @DisplayName("Rule 7: Negative remaining balance is preserved (over-allocation)")
    void testNegativeRemainingBalancePreserved() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(openAssessment));

        AssessmentDto.Response resp = assessmentService.getAssessmentById(1L);

        assertEquals(new BigDecimal("-665.62"), resp.getRemainingBalance(), "Negative remaining balance must be preserved");
    }

    @Test
    @DisplayName("Rule 5: Cannot edit amount or year on CLOSED assessment")
    void testClosedYearModificationRejected() {
        when(assessmentRepository.findById(2L)).thenReturn(Optional.of(closedAssessment));

        AssessmentDto.Request req = AssessmentDto.Request.builder()
                .zakatYear(2024)
                .zakatAmount(new BigDecimal("2000000.00")) // attempting to change amount
                .carryForward(BigDecimal.ZERO)
                .status("CLOSED")
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                assessmentService.updateAssessment(2L, req)
        );

        assertTrue(ex.getMessage().contains("This Zakat year is CLOSED. Re-open it to change year, amount or carry-forward."));
    }

    @Test
    @DisplayName("Rule 29: Cannot delete assessment if allocations exist")
    void testDeleteAssessmentWithAllocationsFails() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(openAssessment));
        when(allocationRepository.existsByAssessmentId(1L)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                assessmentService.deleteAssessment(1L)
        );

        assertTrue(ex.getMessage().contains("Cannot delete assessment because allocations exist"));
    }
}
