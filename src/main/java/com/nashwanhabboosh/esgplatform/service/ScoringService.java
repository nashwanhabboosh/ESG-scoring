package com.nashwanhabboosh.esgplatform.service;

import com.nashwanhabboosh.esgplatform.model.ESGScorecard;
import com.nashwanhabboosh.esgplatform.model.RawMetric;
import com.nashwanhabboosh.esgplatform.model.ScoringMethodology;
import com.nashwanhabboosh.esgplatform.repository.ESGScorecardRepository;
import com.nashwanhabboosh.esgplatform.repository.RawMetricRepository;
import com.nashwanhabboosh.esgplatform.repository.ScoringMethodologyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final RawMetricRepository rawMetricRepository;
    private final ESGScorecardRepository scorecardRepository;
    private final ScoringMethodologyRepository methodologyRepository;

    // Environmental
    private static final Map<String, Double> E_METRICS = Map.of(
        "carbon_emissions",     0.55,
        "renewable_energy_pct", 0.45
    );

    // Social
    private static final Map<String, Double> S_METRICS = Map.of(
        "employee_satisfaction", 0.5,
        "data_privacy_score",    0.5
    );

    // Governance
    private static final Map<String, Double> G_METRICS = Map.of(
        "board_diversity",      0.5,
        "executive_pay_ratio",  0.5
    );

    public ESGScorecard scoreCompany(String companyId) {
        ScoringMethodology methodology = methodologyRepository.findByActive(true)
            .orElseThrow(() -> new IllegalStateException("No active scoring methodology found"));

        return scoreCompany(companyId, methodology);
    }

    public ESGScorecard scoreCompany(String companyId, ScoringMethodology methodology) {
        List<RawMetric> metrics = rawMetricRepository.findByCompanyIdAndYear(companyId, 2024);

        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("No metrics found for company: " + companyId);
        }

        // Convert list to (metricName, value) map for lookups
        Map<String, Double> metricMap = metrics.stream()
            .collect(Collectors.toMap(RawMetric::getMetricName, RawMetric::getValue));

        double eScore = computePillarScore(metricMap, E_METRICS);
        double sScore = computePillarScore(metricMap, S_METRICS);
        double gScore = computePillarScore(metricMap, G_METRICS);

        double composite = (eScore * methodology.getEnvironmentalWeight())
                         + (sScore * methodology.getSocialWeight())
                         + (gScore * methodology.getGovernanceWeight());

        ESGScorecard scorecard = new ESGScorecard();
        scorecard.setCompanyId(companyId);
        scorecard.setMethodologyId(methodology.getId());
        scorecard.setEnvironmentalScore(round(eScore));
        scorecard.setSocialScore(round(sScore));
        scorecard.setGovernanceScore(round(gScore));
        scorecard.setCompositeScore(round(composite));
        scorecard.setTimestamp(LocalDateTime.now());

        return scorecardRepository.save(scorecard);
    }

    public List<ESGScorecard> scoreAllCompanies() {
        ScoringMethodology methodology = methodologyRepository.findByActive(true)
            .orElseThrow(() -> new IllegalStateException("No active scoring methodology found"));

        List<String> companyIds = rawMetricRepository.findAll()
            .stream()
            .map(RawMetric::getCompanyId)
            .distinct()
            .collect(Collectors.toList());

        log.info("Scoring {} companies with methodology: {}", companyIds.size(), methodology.getName());

        return companyIds.stream()
            .map(id -> scoreCompany(id, methodology))
            .collect(Collectors.toList());
    }

    private double computePillarScore(Map<String, Double> metricMap, Map<String, Double> pillarWeights) {
        double score = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : pillarWeights.entrySet()) {
            String metricName = entry.getKey();
            double weight = entry.getValue();

            if (metricMap.containsKey(metricName)) {
                score += metricMap.get(metricName) * weight;
                totalWeight += weight;
            }
        }

        // Normalization
        return totalWeight > 0 ? score / totalWeight : 0.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}