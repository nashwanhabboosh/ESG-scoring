package com.nashwanhabboosh.esgplatform.controller;

import com.nashwanhabboosh.esgplatform.dto.SectorSummary;
import com.nashwanhabboosh.esgplatform.service.SectorAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sectors")
@RequiredArgsConstructor
public class SectorAnalyticsController {

    private final SectorAnalyticsService sectorAnalyticsService;

    @GetMapping
    public List<SectorSummary> getAllSectors() {
        return sectorAnalyticsService.getAllSectorSummaries();
    }

    @GetMapping("/{sector}")
    public SectorSummary getSectorDetail(@PathVariable String sector) {
        return sectorAnalyticsService.getSectorDetail(sector);
    }
}