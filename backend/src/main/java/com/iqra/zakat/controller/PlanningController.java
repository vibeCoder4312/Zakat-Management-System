package com.iqra.zakat.controller;

import com.iqra.zakat.dto.AllocationDto;
import com.iqra.zakat.dto.PlanningDto;
import com.iqra.zakat.service.PlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planning")
@RequiredArgsConstructor
@Tag(name = "Zakat Planning", description = "Planning records and live execution comparisons")
public class PlanningController {

    private final PlanningService planningService;

    @GetMapping
    @Operation(summary = "Get live planning report by year(s)")
    public ResponseEntity<List<PlanningDto.Response>> list(@RequestParam(required = false) List<Integer> years) {
        return ResponseEntity.ok(planningService.getPlanningList(years));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get planning record by ID")
    public ResponseEntity<PlanningDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(planningService.getById(id));
    }

    @GetMapping("/{id}/allocations")
    @Operation(summary = "Get drilldown matching allocations for this planning record")
    public ResponseEntity<List<AllocationDto.Response>> getDrilldown(@PathVariable Long id) {
        return ResponseEntity.ok(planningService.getDrilldownAllocations(id));
    }

    @PostMapping
    @Operation(summary = "Create planning record (ADMIN only)")
    public ResponseEntity<PlanningDto.Response> create(@Valid @RequestBody PlanningDto.Request req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planningService.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update planning record (ADMIN only)")
    public ResponseEntity<PlanningDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody PlanningDto.Request req) {
        return ResponseEntity.ok(planningService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete planning record (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planningService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
