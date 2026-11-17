package com.iqra.zakat.controller;

import com.iqra.zakat.dto.DashboardDto;
import com.iqra.zakat.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Home dashboard widgets and dynamic pivots")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get full dashboard metrics, hero, KPIs, and chart data for selected years")
    public ResponseEntity<DashboardDto.FullDashboard> getDashboard(
            @RequestParam(required = false) List<Integer> years) {
        return ResponseEntity.ok(dashboardService.getDashboard(years));
    }

    @GetMapping("/pivot/classification-by-year")
    @Operation(summary = "Get dynamic Classification × Year pivot table")
    public ResponseEntity<DashboardDto.PivotTable> getClassificationByYearPivot() {
        return ResponseEntity.ok(dashboardService.getClassificationByYearPivot());
    }

    @GetMapping("/pivot/category-by-year")
    @Operation(summary = "Get dynamic Category × Year pivot table")
    public ResponseEntity<DashboardDto.PivotTable> getCategoryByYearPivot() {
        return ResponseEntity.ok(dashboardService.getCategoryByYearPivot());
    }
}
