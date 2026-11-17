package com.iqra.zakat.service;

import com.iqra.zakat.dto.AllocationDto;
import com.iqra.zakat.dto.PlanningDto;
import com.iqra.zakat.entity.Allocation;
import com.iqra.zakat.entity.Classification;
import com.iqra.zakat.entity.Planning;
import com.iqra.zakat.exception.BusinessRuleException;
import com.iqra.zakat.exception.ResourceNotFoundException;
import com.iqra.zakat.mapper.EntityMappers;
import com.iqra.zakat.repository.AllocationRepository;
import com.iqra.zakat.repository.ClassificationRepository;
import com.iqra.zakat.repository.PlanningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanningService {

    private final PlanningRepository planningRepository;
    private final AllocationRepository allocationRepository;
    private final ClassificationRepository classificationRepository;
    private final EntityMappers mapper;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<PlanningDto.Response> getPlanningList(List<Integer> years) {
        List<Planning> plans = (years != null && !years.isEmpty())
                ? planningRepository.findByYears(years)
                : planningRepository.findAll();

        if (plans.isEmpty()) {
            return Collections.emptyList();
        }

        int minYear = plans.stream().mapToInt(Planning::getZakatYear).min().orElse(2020);
        int maxYear = plans.stream().mapToInt(Planning::getZakatYear).max().orElse(2026);

        Map<String, BigDecimal> historicalPaidMap = prefetchHistoricalPaidSums(minYear - 5, maxYear - 1);

        return plans.stream().map(p -> enrichPlanningResponse(p, historicalPaidMap)).toList();
    }

    @Transactional(readOnly = true)
    public PlanningDto.Response getById(Long id) {
        Planning p = planningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Planning", id));
        Map<String, BigDecimal> historicalPaidMap = prefetchHistoricalPaidSums(p.getZakatYear() - 5, p.getZakatYear() - 1);
        return enrichPlanningResponse(p, historicalPaidMap);
    }

    @Transactional(readOnly = true)
    public List<AllocationDto.Response> getDrilldownAllocations(Long planningId) {
        Planning p = planningRepository.findById(planningId)
                .orElseThrow(() -> new ResourceNotFoundException("Planning", planningId));

        if (p.getClassificationId() == null || p.getCategory() == null) {
            return Collections.emptyList();
        }

        List<Allocation> matching = allocationRepository.findMatchingForPlanning(
                p.getZakatYear(), p.getClassificationId(), p.getCategory().trim());

        return matching.stream().map(mapper::toAllocationResponse).toList();
    }

    @Transactional
    public PlanningDto.Response create(PlanningDto.Request req) {
        Classification cls = null;
        String fundType = "ZAKAT";
        if (req.getClassificationId() != null) {
            cls = classificationRepository.findById(req.getClassificationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classification", req.getClassificationId()));
            fundType = cls.getFundType(); // Rule 33
        }

        Planning entity = Planning.builder()
                .zakatYear(req.getZakatYear())
                .planType(fundType)
                .classificationId(req.getClassificationId())
                .category(req.getCategory())
                .plannedAmount(req.getPlannedAmount() != null ? req.getPlannedAmount() : BigDecimal.ZERO)
                .remarks(req.getRemarks())
                .build();

        String username = authService.getCurrentUsername();
        entity.setCreatedBy(username);
        entity.setCreatedOn(OffsetDateTime.now());
        entity.setUpdatedBy(username);
        entity.setUpdatedOn(OffsetDateTime.now());

        Planning saved = planningRepository.save(entity);
        Map<String, BigDecimal> historicalPaidMap = prefetchHistoricalPaidSums(saved.getZakatYear() - 5, saved.getZakatYear() - 1);
        return enrichPlanningResponse(saved, historicalPaidMap);
    }

    @Transactional
    public PlanningDto.Response update(Long id, PlanningDto.Request req) {
        Planning existing = planningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Planning", id));

        if (req.getClassificationId() != null) {
            Classification cls = classificationRepository.findById(req.getClassificationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Classification", req.getClassificationId()));
            existing.setClassificationId(cls.getClassificationId());
            existing.setPlanType(cls.getFundType()); // Rule 33
        }

        existing.setZakatYear(req.getZakatYear());
        existing.setCategory(req.getCategory());
        existing.setPlannedAmount(req.getPlannedAmount() != null ? req.getPlannedAmount() : BigDecimal.ZERO);
        existing.setRemarks(req.getRemarks());

        existing.setUpdatedBy(authService.getCurrentUsername());
        existing.setUpdatedOn(OffsetDateTime.now());

        Planning saved = planningRepository.save(existing);
        Map<String, BigDecimal> historicalPaidMap = prefetchHistoricalPaidSums(saved.getZakatYear() - 5, saved.getZakatYear() - 1);
        return enrichPlanningResponse(saved, historicalPaidMap);
    }

    @Transactional
    public void delete(Long id) {
        Planning existing = planningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Planning", id));
        planningRepository.delete(existing);
    }

    private Map<String, BigDecimal> prefetchHistoricalPaidSums(int minYear, int maxYear) {
        List<Object[]> rows = allocationRepository.sumPaidByYearClassificationAndCategory(minYear, maxYear);
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            Integer year = (Integer) row[0];
            Long clsId = (Long) row[1];
            String cat = (String) row[2];
            BigDecimal sum = (BigDecimal) row[3];
            if (year != null && clsId != null && cat != null) {
                String key = year + "_" + clsId + "_" + cat.trim().toLowerCase();
                map.put(key, sum != null ? sum : BigDecimal.ZERO);
            }
        }
        return map;
    }

    private BigDecimal getPriorYearSum(Map<String, BigDecimal> map, int year, Long clsId, String cat) {
        if (map == null || clsId == null || cat == null || cat.isBlank()) {
            return BigDecimal.ZERO;
        }
        String key = year + "_" + clsId + "_" + cat.trim().toLowerCase();
        return map.getOrDefault(key, BigDecimal.ZERO);
    }

    /**
     * Rules 35, 36, 37: Enriches stored planning input with live calculated allocation values.
     */
    private PlanningDto.Response enrichPlanningResponse(Planning p, Map<String, BigDecimal> historicalPaidMap) {
        BigDecimal planned = p.getPlannedAmount() != null ? p.getPlannedAmount() : BigDecimal.ZERO;

        String clsName = null;
        if (p.getClassificationEntity() != null) {
            clsName = p.getClassificationEntity().getName();
        } else if (p.getClassificationId() != null) {
            clsName = classificationRepository.findById(p.getClassificationId())
                    .map(Classification::getName).orElse(null);
        }

        List<Allocation> matching = Collections.emptyList();
        if (p.getClassificationId() != null && p.getCategory() != null) {
            matching = allocationRepository.findMatchingForPlanning(
                    p.getZakatYear(), p.getClassificationId(), p.getCategory().trim());
        }

        BigDecimal paid = BigDecimal.ZERO;
        Set<Long> uniqueBenIds = new HashSet<>();
        Set<String> uniqueSources = new LinkedHashSet<>();
        LocalDate maxPaidOn = null;

        long allocationCount = 0;
        for (Allocation a : matching) {
            if (a.getPaidAmount() != null) {
                paid = paid.add(a.getPaidAmount());
            }
            // Zero-amount placeholder rows (imported PLANNED rows with no amount) are not counted,
            // matching the Allocation/Beneficiary Count columns of the APEX planning report.
            boolean hasAmount = (a.getPaidAmount() != null && a.getPaidAmount().signum() > 0)
                    || (a.getPlannedAmount() != null && a.getPlannedAmount().signum() > 0);
            if (!hasAmount) {
                continue;
            }
            allocationCount++;
            if (a.getBeneficiaryId() != null) {
                uniqueBenIds.add(a.getBeneficiaryId());
            }
            if (a.getPaidSourceAccount() != null && !a.getPaidSourceAccount().isBlank()) {
                uniqueSources.add(a.getPaidSourceAccount().trim());
            }
            if (a.getPaidOn() != null) {
                if (maxPaidOn == null || a.getPaidOn().isAfter(maxPaidOn)) {
                    maxPaidOn = a.getPaidOn();
                }
            }
        }

        BigDecimal remaining = planned.subtract(paid);
        BigDecimal outstanding = planned.subtract(paid).max(BigDecimal.ZERO);

        String paymentStatus;
        if (paid.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = "PLANNED";
        } else if (paid.compareTo(planned) >= 0) {
            paymentStatus = "PAID";
        } else {
            paymentStatus = "PARTIAL";
        }

        String sourceAccountStr = String.join(", ", uniqueSources);

        // Previous year amounts from pre-fetched map (Rule 37)
        int yr = p.getZakatYear();
        Long clsId = p.getClassificationId();
        String cat = p.getCategory();

        BigDecimal cy1 = getPriorYearSum(historicalPaidMap, yr - 1, clsId, cat);
        BigDecimal cy2 = getPriorYearSum(historicalPaidMap, yr - 2, clsId, cat);
        BigDecimal cy3 = getPriorYearSum(historicalPaidMap, yr - 3, clsId, cat);
        BigDecimal cy4 = getPriorYearSum(historicalPaidMap, yr - 4, clsId, cat);
        BigDecimal cy5 = getPriorYearSum(historicalPaidMap, yr - 5, clsId, cat);

        return PlanningDto.Response.builder()
                .planningId(p.getPlanningId())
                .zakatYear(p.getZakatYear())
                .planType(p.getPlanType())
                .classificationId(p.getClassificationId())
                .classification(clsName)
                .category(p.getCategory())
                .plannedAmount(planned)
                .paidAmount(paid)
                .remainingAmount(remaining)
                .outstandingAmount(outstanding)
                .allocationCount(allocationCount)
                .beneficiaryCount((long) uniqueBenIds.size())
                .sourceAccount(sourceAccountStr)
                .lastPaymentDate(maxPaidOn)
                .paymentStatus(paymentStatus)
                .cyMinus1(cy1)
                .cyMinus2(cy2)
                .cyMinus3(cy3)
                .cyMinus4(cy4)
                .cyMinus5(cy5)
                .remarks(p.getRemarks())
                .createdBy(p.getCreatedBy())
                .createdOn(p.getCreatedOn())
                .updatedBy(p.getUpdatedBy())
                .updatedOn(p.getUpdatedOn())
                .build();
    }
}
