package com.iqra.zakat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DashboardDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HeroData {
        private String yearTitle; // e.g. "Zakat 2026" or "Zakat 2025, 2026"
        private String statusPill; // "OPEN", "CLOSED", or "N years combined"
        private BigDecimal zakatPaid;
        private BigDecimal netPayable;
        private BigDecimal zakatAmount;
        private BigDecimal carryForward;
        private BigDecimal sadaqahPaid;
        private BigDecimal obligationPercentage;
        private BigDecimal plannedOutstanding;
        private BigDecimal unallocated;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KpiData {
        private BigDecimal netPayable;
        private long paidCount;
        private BigDecimal plannedNotPaidAmount;
        private long plannedNotPaidCount;
        private BigDecimal remainingOrOverallocated;
        private boolean isOverallocated;
        private BigDecimal distributedPercentage;
        private long beneficiariesHelped;
        private long verifiedActiveCount;
        private long awaitingVerificationCount;
        private long missingReceiptsCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartItem {
        private String name;
        private BigDecimal paid;
        private BigDecimal pending;
        private BigDecimal total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyItem {
        private String monthYear; // e.g. "Jul 2020"
        private BigDecimal amount;
        private BigDecimal runningTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class YearlyItem {
        private Integer year;
        private BigDecimal netPayable;
        private BigDecimal zakatPaid;
        private BigDecimal sadaqahPaid;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DonutItem {
        private String label;
        private BigDecimal amount;
        private BigDecimal percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttentionItem {
        private String badgeType; // "PENDING_PAYMENT", "RECEIPT_MISSING", "VERIFY_BENEFICIARY"
        private String badgeLabel; // "Payment pending", "Receipt missing", "Verify beneficiary"
        private String beneficiary;
        private String detail;
        private BigDecimal amount;
        private Long entityId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentPaymentItem {
        private LocalDate paidOn;
        private String beneficiary;
        private String classification;
        private String sourceAccount;
        private String paymentMethod;
        private BigDecimal paidAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerificationCounts {
        private long verified;
        private long pending;
        private long cancelled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PivotTable {
        private String title;
        private List<Integer> years;
        private List<PivotRow> rows;
        private Map<Integer, BigDecimal> yearTotals;
        private BigDecimal grandTotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PivotRow {
        private String rowLabel;
        private Map<Integer, BigDecimal> values; // year -> amount
        private BigDecimal total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FullDashboard {
        private HeroData hero;
        private KpiData kpis;
        private List<ChartItem> distributionByClassification;
        private List<ChartItem> distributionByCategory;
        private List<MonthlyItem> monthlyDistribution;
        private List<YearlyItem> yearlyDistribution;
        private List<DonutItem> beneficiaryCategoryDonut;
        private List<DonutItem> cashVsOnlineDonut;
        private List<DonutItem> sourceAccountDistribution;
        private List<AttentionItem> needsAttention;
        private List<RecentPaymentItem> recentPayments;
        private VerificationCounts verificationCounts;
    }
}
