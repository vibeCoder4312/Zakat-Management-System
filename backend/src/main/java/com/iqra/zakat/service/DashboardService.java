package com.iqra.zakat.service;

import com.iqra.zakat.dto.DashboardDto;
import com.iqra.zakat.entity.Allocation;
import com.iqra.zakat.entity.Assessment;
import com.iqra.zakat.entity.Beneficiary;
import com.iqra.zakat.entity.Classification;
import com.iqra.zakat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final AssessmentRepository assessmentRepository;
    private final AllocationRepository allocationRepository;
    private final ClassificationRepository classificationRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SourceAccountRepository sourceAccountRepository;

    @Transactional(readOnly = true)
    public DashboardDto.FullDashboard getDashboard(List<Integer> years) {
        List<Integer> selectedYears = (years == null || years.isEmpty())
                ? assessmentRepository.findAllDistinctYears()
                : years;

        List<Assessment> assessments = assessmentRepository.findAll().stream()
                .filter(a -> selectedYears.contains(a.getZakatYear()))
                .sorted(Comparator.comparing(Assessment::getZakatYear).reversed())
                .toList();

        List<Allocation> allAllocations = allocationRepository.findActiveByYearsWithClassification(selectedYears);

        // 1. Hero Data
        DashboardDto.HeroData hero = buildHeroData(selectedYears, assessments, allAllocations);

        // 2. KPI Cards
        DashboardDto.KpiData kpis = buildKpiData(assessments, allAllocations);

        // 3. Distribution by Classification
        List<DashboardDto.ChartItem> distByClass = buildDistributionByClassification(allAllocations);

        // 4. Distribution by Category
        List<DashboardDto.ChartItem> distByCategory = buildDistributionByCategory(allAllocations);

        // 5. Monthly Distribution
        List<DashboardDto.MonthlyItem> monthly = buildMonthlyDistribution(allAllocations);

        // 6. Yearly Distribution
        List<DashboardDto.YearlyItem> yearly = buildYearlyDistribution(assessments, allAllocations);

        // 7. Beneficiary Category Donut
        List<DashboardDto.DonutItem> benCatDonut = buildBeneficiaryCategoryDonut(allAllocations);

        // 8. Cash vs Online Donut
        List<DashboardDto.DonutItem> cashOnlineDonut = buildCashVsOnlineDonut(allAllocations);

        // 9. Paid from Source Account
        List<DashboardDto.DonutItem> srcAcctDist = buildSourceAccountDistribution(allAllocations);

        // 10. Needs Attention (max 12 rows)
        List<DashboardDto.AttentionItem> attention = buildNeedsAttention(allAllocations);

        // 11. Recent Payments (8 rows)
        List<DashboardDto.RecentPaymentItem> recentPayments = buildRecentPayments(allAllocations);

        // 12. Beneficiary Verification (not year filtered)
        DashboardDto.VerificationCounts verCounts = buildVerificationCounts();

        return DashboardDto.FullDashboard.builder()
                .hero(hero)
                .kpis(kpis)
                .distributionByClassification(distByClass)
                .distributionByCategory(distByCategory)
                .monthlyDistribution(monthly)
                .yearlyDistribution(yearly)
                .beneficiaryCategoryDonut(benCatDonut)
                .cashVsOnlineDonut(cashOnlineDonut)
                .sourceAccountDistribution(srcAcctDist)
                .needsAttention(attention)
                .recentPayments(recentPayments)
                .verificationCounts(verCounts)
                .build();
    }

    private DashboardDto.HeroData buildHeroData(List<Integer> years, List<Assessment> assessments, List<Allocation> allocations) {
        String yearTitle = years.size() == 1
                ? "Zakat " + years.get(0)
                : "Zakat " + years.stream().sorted().map(String::valueOf).collect(Collectors.joining(", "));

        String statusPill = years.size() == 1 && !assessments.isEmpty()
                ? assessments.get(0).getStatus()
                : years.size() + " years combined";

        BigDecimal zakatPaid = BigDecimal.ZERO;
        BigDecimal sadaqahPaid = BigDecimal.ZERO;
        BigDecimal plannedOutstanding = BigDecimal.ZERO;

        for (Allocation a : allocations) {
            String fundType = a.getClassificationEntity() != null ? a.getClassificationEntity().getFundType() : "ZAKAT";
            if ("ZAKAT".equals(fundType)) {
                if ("PAID".equals(a.getStatus())) {
                    zakatPaid = zakatPaid.add(a.getPaidAmount());
                } else if ("PLANNED".equals(a.getStatus())) {
                    plannedOutstanding = plannedOutstanding.add(a.getPlannedAmount().subtract(a.getPaidAmount()).max(BigDecimal.ZERO));
                }
            } else if ("SADAQAH".equals(fundType)) {
                if ("PAID".equals(a.getStatus())) {
                    sadaqahPaid = sadaqahPaid.add(a.getPaidAmount());
                }
            }
        }

        BigDecimal netPayable = assessments.stream()
                .map(Assessment::getNetPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal zakatAmount = assessments.stream()
                .map(Assessment::getZakatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal carryForward = assessments.stream()
                .map(Assessment::getCarryForward)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unallocated = netPayable.subtract(zakatPaid).subtract(plannedOutstanding);

        BigDecimal obligationPct = BigDecimal.ZERO;
        if (netPayable.compareTo(BigDecimal.ZERO) > 0) {
            obligationPct = zakatPaid.multiply(BigDecimal.valueOf(100)).divide(netPayable, 1, RoundingMode.HALF_UP);
        }

        return DashboardDto.HeroData.builder()
                .yearTitle(yearTitle)
                .statusPill(statusPill)
                .zakatPaid(zakatPaid)
                .netPayable(netPayable)
                .zakatAmount(zakatAmount)
                .carryForward(carryForward)
                .sadaqahPaid(sadaqahPaid)
                .obligationPercentage(obligationPct)
                .plannedOutstanding(plannedOutstanding)
                .unallocated(unallocated)
                .build();
    }

    private DashboardDto.KpiData buildKpiData(List<Assessment> assessments, List<Allocation> allocations) {
        BigDecimal netPayable = assessments.stream()
                .map(Assessment::getNetPayable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount = 0;
        BigDecimal plannedNotPaidAmt = BigDecimal.ZERO;
        long plannedNotPaidCount = 0;
        long missingReceipts = 0;
        Set<Long> paidBenIds = new HashSet<>();

        for (Allocation a : allocations) {
            if ("PAID".equals(a.getStatus())) {
                paidCount++;
                if ("N".equalsIgnoreCase(a.getReceiptReceived())) {
                    missingReceipts++;
                }
                if (a.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 && a.getBeneficiaryId() != null) {
                    paidBenIds.add(a.getBeneficiaryId());
                }
            } else if ("PLANNED".equals(a.getStatus())) {
                BigDecimal rem = a.getPlannedAmount().subtract(a.getPaidAmount()).max(BigDecimal.ZERO);
                if (rem.compareTo(BigDecimal.ZERO) > 0) {
                    plannedNotPaidAmt = plannedNotPaidAmt.add(rem);
                    plannedNotPaidCount++;
                }
            }
        }

        BigDecimal totalAllocated = assessments.stream()
                .map(Assessment::getTotalAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingBal = netPayable.subtract(totalAllocated);
        boolean isOver = remainingBal.compareTo(BigDecimal.ZERO) < 0;

        BigDecimal distributedPct = BigDecimal.ZERO;
        if (netPayable.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalZakatPaid = allocations.stream()
                    .filter(a -> "PAID".equals(a.getStatus()))
                    .filter(a -> a.getClassificationEntity() == null || "ZAKAT".equals(a.getClassificationEntity().getFundType()))
                    .map(Allocation::getPaidAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            distributedPct = totalZakatPaid.multiply(BigDecimal.valueOf(100)).divide(netPayable, 1, RoundingMode.HALF_UP);
        }

        long verifiedActive = beneficiaryRepository.countByVerificationStatusAndIsActive("VERIFIED", "Y");
        long awaitingVerification = beneficiaryRepository.countByVerificationStatusAndIsActive("PENDING", "Y");

        return DashboardDto.KpiData.builder()
                .netPayable(netPayable)
                .paidCount(paidCount)
                .plannedNotPaidAmount(plannedNotPaidAmt)
                .plannedNotPaidCount(plannedNotPaidCount)
                .remainingOrOverallocated(remainingBal.abs())
                .isOverallocated(isOver)
                .distributedPercentage(distributedPct)
                .beneficiariesHelped(paidBenIds.size())
                .verifiedActiveCount(verifiedActive)
                .awaitingVerificationCount(awaitingVerification)
                .missingReceiptsCount(missingReceipts)
                .build();
    }

    private List<DashboardDto.ChartItem> buildDistributionByClassification(List<Allocation> allocations) {
        Map<String, BigDecimal[]> map = new LinkedHashMap<>();

        for (Allocation a : allocations) {
            String name = a.getClassification() != null ? a.getClassification() : "Unclassified";
            map.putIfAbsent(name, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if ("PAID".equals(a.getStatus())) {
                map.get(name)[0] = map.get(name)[0].add(a.getPaidAmount());
            } else if ("PLANNED".equals(a.getStatus())) {
                map.get(name)[1] = map.get(name)[1].add(a.getPlannedAmount().subtract(a.getPaidAmount()).max(BigDecimal.ZERO));
            }
        }

        return map.entrySet().stream()
                .map(e -> DashboardDto.ChartItem.builder()
                        .name(e.getKey())
                        .paid(e.getValue()[0])
                        .pending(e.getValue()[1])
                        .total(e.getValue()[0].add(e.getValue()[1]))
                        .build())
                .sorted(Comparator.comparing(DashboardDto.ChartItem::getTotal).reversed())
                .toList();
    }

    private List<DashboardDto.ChartItem> buildDistributionByCategory(List<Allocation> allocations) {
        Map<String, BigDecimal[]> map = new LinkedHashMap<>();

        for (Allocation a : allocations) {
            String name = a.getCategory() != null && !a.getCategory().isBlank() ? a.getCategory() : "Uncategorized";
            map.putIfAbsent(name, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if ("PAID".equals(a.getStatus())) {
                map.get(name)[0] = map.get(name)[0].add(a.getPaidAmount());
            } else if ("PLANNED".equals(a.getStatus())) {
                map.get(name)[1] = map.get(name)[1].add(a.getPlannedAmount().subtract(a.getPaidAmount()).max(BigDecimal.ZERO));
            }
        }

        return map.entrySet().stream()
                .map(e -> DashboardDto.ChartItem.builder()
                        .name(e.getKey())
                        .paid(e.getValue()[0])
                        .pending(e.getValue()[1])
                        .total(e.getValue()[0].add(e.getValue()[1]))
                        .build())
                .sorted(Comparator.comparing(DashboardDto.ChartItem::getTotal).reversed())
                .toList();
    }

    private List<DashboardDto.MonthlyItem> buildMonthlyDistribution(List<Allocation> allocations) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");
        Map<String, BigDecimal> monthlyTotals = new LinkedHashMap<>();

        allocations.stream()
                .filter(a -> a.getPaidOn() != null && "PAID".equals(a.getStatus()))
                .sorted(Comparator.comparing(Allocation::getPaidOn))
                .forEach(a -> {
                    String my = a.getPaidOn().format(fmt);
                    monthlyTotals.put(my, monthlyTotals.getOrDefault(my, BigDecimal.ZERO).add(a.getPaidAmount()));
                });

        List<DashboardDto.MonthlyItem> list = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : monthlyTotals.entrySet()) {
            running = running.add(entry.getValue());
            list.add(DashboardDto.MonthlyItem.builder()
                    .monthYear(entry.getKey())
                    .amount(entry.getValue())
                    .runningTotal(running)
                    .build());
        }
        return list;
    }

    private List<DashboardDto.YearlyItem> buildYearlyDistribution(List<Assessment> assessments, List<Allocation> allocations) {
        Map<Integer, BigDecimal[]> map = new LinkedHashMap<>();
        for (Assessment a : assessments) {
            map.put(a.getZakatYear(), new BigDecimal[]{a.getNetPayable(), BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Allocation a : allocations) {
            if ("PAID".equals(a.getStatus()) && map.containsKey(a.getZakatYear())) {
                String ft = a.getClassificationEntity() != null ? a.getClassificationEntity().getFundType() : "ZAKAT";
                if ("ZAKAT".equals(ft)) {
                    map.get(a.getZakatYear())[1] = map.get(a.getZakatYear())[1].add(a.getPaidAmount());
                } else {
                    map.get(a.getZakatYear())[2] = map.get(a.getZakatYear())[2].add(a.getPaidAmount());
                }
            }
        }

        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> DashboardDto.YearlyItem.builder()
                        .year(e.getKey())
                        .netPayable(e.getValue()[0])
                        .zakatPaid(e.getValue()[1])
                        .sadaqahPaid(e.getValue()[2])
                        .build())
                .toList();
    }

    private List<DashboardDto.DonutItem> buildBeneficiaryCategoryDonut(List<Allocation> allocations) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Allocation a : allocations) {
            String cat = a.getCategory() != null && !a.getCategory().isBlank() ? a.getCategory() : "Other";
            BigDecimal amt = "PAID".equals(a.getStatus()) ? a.getPaidAmount() : a.getPlannedAmount();
            map.put(cat, map.getOrDefault(cat, BigDecimal.ZERO).add(amt));
            grandTotal = grandTotal.add(amt);
        }

        BigDecimal total = grandTotal;
        return map.entrySet().stream()
                .map(e -> {
                    BigDecimal pct = total.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return DashboardDto.DonutItem.builder()
                            .label(e.getKey())
                            .amount(e.getValue())
                            .percentage(pct)
                            .build();
                })
                .sorted(Comparator.comparing(DashboardDto.DonutItem::getAmount).reversed())
                .toList();
    }

    private List<DashboardDto.DonutItem> buildCashVsOnlineDonut(List<Allocation> allocations) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Allocation a : allocations) {
            if ("PAID".equals(a.getStatus())) {
                String m = a.getPaymentMethod() != null && !a.getPaymentMethod().isBlank()
                        ? a.getPaymentMethod() : "Not set";
                map.put(m, map.getOrDefault(m, BigDecimal.ZERO).add(a.getPaidAmount()));
                grandTotal = grandTotal.add(a.getPaidAmount());
            }
        }

        BigDecimal total = grandTotal;
        return map.entrySet().stream()
                .map(e -> {
                    BigDecimal pct = total.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return DashboardDto.DonutItem.builder()
                            .label(e.getKey())
                            .amount(e.getValue())
                            .percentage(pct)
                            .build();
                })
                .sorted(Comparator.comparing(DashboardDto.DonutItem::getAmount).reversed())
                .toList();
    }

    private List<DashboardDto.DonutItem> buildSourceAccountDistribution(List<Allocation> allocations) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();

        for (Allocation a : allocations) {
            if ("PAID".equals(a.getStatus())) {
                String s = a.getPaidSourceAccount() != null && !a.getPaidSourceAccount().isBlank()
                        ? a.getPaidSourceAccount() : "Not specified";
                map.put(s, map.getOrDefault(s, BigDecimal.ZERO).add(a.getPaidAmount()));
            }
        }

        BigDecimal total = map.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return map.entrySet().stream()
                .map(e -> DashboardDto.DonutItem.builder()
                        .label(e.getKey())
                        .amount(e.getValue())
                        .percentage(total.signum() > 0
                                ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .sorted(Comparator.comparing(DashboardDto.DonutItem::getAmount).reversed())
                .toList();
    }

    private List<DashboardDto.AttentionItem> buildNeedsAttention(List<Allocation> allocations) {
        List<DashboardDto.AttentionItem> list = new ArrayList<>();

        // 1. Payment pending (non-cancelled with outstanding amount)
        for (Allocation a : allocations) {
            if ("PLANNED".equals(a.getStatus())) {
                BigDecimal outstanding = a.getPlannedAmount().subtract(a.getPaidAmount()).max(BigDecimal.ZERO);
                if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                    list.add(DashboardDto.AttentionItem.builder()
                            .badgeType("PENDING_PAYMENT")
                            .badgeLabel("Payment pending")
                            .beneficiary(a.getBeneficiary())
                            .detail(a.getClassification())
                            .amount(outstanding)
                            .entityId(a.getAllocationId())
                            .build());
                }
            }
        }

        // 2. Receipt missing (PAID with receipt_received = 'N')
        for (Allocation a : allocations) {
            if ("PAID".equals(a.getStatus()) && "N".equalsIgnoreCase(a.getReceiptReceived())) {
                list.add(DashboardDto.AttentionItem.builder()
                        .badgeType("RECEIPT_MISSING")
                        .badgeLabel("Receipt missing")
                        .beneficiary(a.getBeneficiary())
                        .detail(a.getClassification())
                        .amount(a.getPaidAmount())
                        .entityId(a.getAllocationId())
                        .build());
            }
        }

        // 3. Verify beneficiary (PENDING and active)
        List<Beneficiary> pendingBens = beneficiaryRepository.searchBeneficiaries(
                null, null, "PENDING", "Y", null, PageRequest.of(0, 12)).getContent();
        for (Beneficiary b : pendingBens) {
            list.add(DashboardDto.AttentionItem.builder()
                    .badgeType("VERIFY_BENEFICIARY")
                    .badgeLabel("Verify beneficiary")
                    .beneficiary(b.getName())
                    .detail(b.getBeneficiaryCategory())
                    .amount(BigDecimal.ZERO)
                    .entityId(b.getBeneficiaryId())
                    .build());
        }

        // Sort: pending -> receipt -> verify, then amount descending
        list.sort((o1, o2) -> {
            int rank1 = rankBadge(o1.getBadgeType());
            int rank2 = rankBadge(o2.getBadgeType());
            if (rank1 != rank2) return Integer.compare(rank1, rank2);
            return o2.getAmount().compareTo(o1.getAmount());
        });

        return list.stream().limit(12).toList();
    }

    private int rankBadge(String type) {
        return switch (type) {
            case "PENDING_PAYMENT" -> 1;
            case "RECEIPT_MISSING" -> 2;
            case "VERIFY_BENEFICIARY" -> 3;
            default -> 4;
        };
    }

    private List<DashboardDto.RecentPaymentItem> buildRecentPayments(List<Allocation> allocations) {
        return allocations.stream()
                .filter(a -> "PAID".equals(a.getStatus()))
                .sorted((a1, a2) -> {
                    if (a1.getPaidOn() == null && a2.getPaidOn() == null) return Long.compare(a2.getAllocationId(), a1.getAllocationId());
                    if (a1.getPaidOn() == null) return 1;
                    if (a2.getPaidOn() == null) return -1;
                    return a2.getPaidOn().compareTo(a1.getPaidOn());
                })
                .limit(8)
                .map(a -> DashboardDto.RecentPaymentItem.builder()
                        .paidOn(a.getPaidOn())
                        .beneficiary(a.getBeneficiary())
                        .classification(a.getClassification())
                        .sourceAccount(a.getPaidSourceAccount())
                        .paymentMethod(a.getPaymentMethod())
                        .paidAmount(a.getPaidAmount())
                        .build())
                .toList();
    }

    private DashboardDto.VerificationCounts buildVerificationCounts() {
        long v = beneficiaryRepository.countByVerificationStatusAndIsActive("VERIFIED", "Y");
        long p = beneficiaryRepository.countByVerificationStatusAndIsActive("PENDING", "Y");
        long c = beneficiaryRepository.countByVerificationStatusAndIsActive("CANCELLED", "Y");
        return DashboardDto.VerificationCounts.builder()
                .verified(v)
                .pending(p)
                .cancelled(c)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardDto.PivotTable getClassificationByYearPivot() {
        List<Object[]> raw = allocationRepository.getClassificationByYearPivotData();
        List<Integer> allYears = assessmentRepository.findAllDistinctYears().stream().sorted().toList();

        Map<String, Map<Integer, BigDecimal>> matrix = new LinkedHashMap<>();
        // Seed active classifications in display order
        classificationRepository.findByIsActiveOrderByDisplayOrderAscNameAsc("Y")
                .forEach(c -> matrix.put(c.getName(), new HashMap<>()));

        for (Object[] r : raw) {
            String cls = (String) r[0];
            Integer yr = ((Number) r[1]).intValue();
            BigDecimal amt = (BigDecimal) r[2];
            matrix.putIfAbsent(cls, new HashMap<>());
            matrix.get(cls).put(yr, amt);
        }

        List<DashboardDto.PivotRow> rows = new ArrayList<>();
        Map<Integer, BigDecimal> colTotals = new HashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map.Entry<String, Map<Integer, BigDecimal>> entry : matrix.entrySet()) {
            BigDecimal rowTotal = BigDecimal.ZERO;
            for (Integer y : allYears) {
                BigDecimal val = entry.getValue().getOrDefault(y, BigDecimal.ZERO);
                rowTotal = rowTotal.add(val);
                colTotals.put(y, colTotals.getOrDefault(y, BigDecimal.ZERO).add(val));
            }
            grandTotal = grandTotal.add(rowTotal);
            rows.add(DashboardDto.PivotRow.builder()
                    .rowLabel(entry.getKey())
                    .values(entry.getValue())
                    .total(rowTotal)
                    .build());
        }

        return DashboardDto.PivotTable.builder()
                .title("Classification × Year (All-Fund Paid Total)")
                .years(allYears)
                .rows(rows)
                .yearTotals(colTotals)
                .grandTotal(grandTotal)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardDto.PivotTable getCategoryByYearPivot() {
        List<Object[]> raw = allocationRepository.getCategoryByYearPivotData();
        List<Integer> allYears = assessmentRepository.findAllDistinctYears().stream().sorted().toList();

        Map<String, Map<Integer, BigDecimal>> matrix = new LinkedHashMap<>();

        for (Object[] r : raw) {
            String cat = (String) r[0];
            Integer yr = ((Number) r[1]).intValue();
            BigDecimal amt = (BigDecimal) r[2];
            matrix.putIfAbsent(cat, new HashMap<>());
            matrix.get(cat).put(yr, amt);
        }

        List<DashboardDto.PivotRow> rows = new ArrayList<>();
        Map<Integer, BigDecimal> colTotals = new HashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map.Entry<String, Map<Integer, BigDecimal>> entry : matrix.entrySet()) {
            BigDecimal rowTotal = BigDecimal.ZERO;
            for (Integer y : allYears) {
                BigDecimal val = entry.getValue().getOrDefault(y, BigDecimal.ZERO);
                rowTotal = rowTotal.add(val);
                colTotals.put(y, colTotals.getOrDefault(y, BigDecimal.ZERO).add(val));
            }
            grandTotal = grandTotal.add(rowTotal);
            rows.add(DashboardDto.PivotRow.builder()
                    .rowLabel(entry.getKey())
                    .values(entry.getValue())
                    .total(rowTotal)
                    .build());
        }

        return DashboardDto.PivotTable.builder()
                .title("Category × Year (All-Fund Paid Total)")
                .years(allYears)
                .rows(rows)
                .yearTotals(colTotals)
                .grandTotal(grandTotal)
                .build();
    }
}
