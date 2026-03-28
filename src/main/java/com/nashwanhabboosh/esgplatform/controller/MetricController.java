package com.nashwanhabboosh.esgplatform.controller;

import com.nashwanhabboosh.esgplatform.model.RawMetric;
import com.nashwanhabboosh.esgplatform.repository.RawMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final RawMetricRepository rawMetricRepository;

    @GetMapping("/company/{companyId}")
    public List<RawMetric> getMetricsByCompany(@PathVariable String companyId) {
        return rawMetricRepository.findByCompanyId(companyId);
    }

    @PostMapping
    public RawMetric createMetric(@RequestBody RawMetric metric) {
        return rawMetricRepository.save(metric);
    }
}