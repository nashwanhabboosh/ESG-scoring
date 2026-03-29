package com.nashwanhabboosh.esgplatform.seeder;

import com.nashwanhabboosh.esgplatform.model.Company;
import com.nashwanhabboosh.esgplatform.model.ESGScorecard;
import com.nashwanhabboosh.esgplatform.model.RawMetric;
import com.nashwanhabboosh.esgplatform.model.ScoringMethodology;
import com.nashwanhabboosh.esgplatform.repository.CompanyRepository;
import com.nashwanhabboosh.esgplatform.repository.ESGScorecardRepository;
import com.nashwanhabboosh.esgplatform.repository.RawMetricRepository;
import com.nashwanhabboosh.esgplatform.repository.ScoringMethodologyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class HistoricalDataSimulator implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final ESGScorecardRepository scorecardRepository;
    private final RawMetricRepository rawMetricRepository;
    private final ScoringMethodologyRepository methodologyRepository;

    // Fixed seed for reproducibility — same history every startup
    private final Random random = new Random(42);

    private static final int MONTHS_OF_HISTORY = 24;
    private static final double DT = 1.0 / 12.0; // monthly time step

    // GBM parameters per sector: {mu_E, mu_S, mu_G, sigma_E, sigma_S, sigma_G}
    // mu = drift (annualized), sigma = volatility (annualized)
    // Renewables drift strongly positive on E, energy slightly negative
    private static final Map<String, double[]> SECTOR_GBM = Map.of(
        "Utilities",   new double[]{ 0.04,  0.01,  0.01,  0.04,  0.03,  0.03},
        "Technology",  new double[]{ 0.02,  0.01,  0.02,  0.05,  0.04,  0.05},
        "Consumer",    new double[]{ 0.01,  0.02,  0.01,  0.03,  0.03,  0.03},
        "Food",        new double[]{ 0.01,  0.01,  0.01,  0.03,  0.03,  0.03},
        "Healthcare",  new double[]{ 0.01,  0.02,  0.01,  0.03,  0.04,  0.03},
        "Finance",     new double[]{ 0.01,  0.01,  0.02,  0.04,  0.03,  0.04},
        "Automotive",  new double[]{ 0.03,  0.01,  0.01,  0.06,  0.04,  0.04},
        "Retail",      new double[]{ 0.01,  0.01,  0.01,  0.04,  0.04,  0.03},
        "Defense",     new double[]{ 0.00,  0.01,  0.01,  0.03,  0.03,  0.03},
        "Energy",      new double[]{-0.01,  0.01,  0.01,  0.05,  0.03,  0.03}
    );

    // Jump diffusion parameters per sector: {lambda, mu_jump, sigma_jump}
    // lambda = expected jumps per year
    // mu_jump = mean jump size (can be negative for scandals)
    // sigma_jump = jump size volatility
    private static final Map<String, double[]> SECTOR_JUMPS = Map.of(
        "Utilities",   new double[]{ 0.5,  3.0,  4.0},  // occasional large green commitments
        "Technology",  new double[]{ 1.0,  0.0,  6.0},  // frequent jumps both ways (privacy scandals, sustainability pledges)
        "Consumer",    new double[]{ 0.5,  1.0,  4.0},
        "Food",        new double[]{ 0.5,  0.5,  3.0},
        "Healthcare",  new double[]{ 0.5,  1.0,  4.0},
        "Finance",     new double[]{ 0.5,  0.5,  5.0},  // governance scandals possible
        "Automotive",  new double[]{ 1.0,  2.0,  6.0},  // EV transition creates large E jumps
        "Retail",      new double[]{ 0.5, -0.5,  5.0},  // labor disputes drag S down
        "Defense",     new double[]{ 0.3,  0.0,  3.0},
        "Energy",      new double[]{ 0.5, -2.0,  5.0}   // mostly negative jumps (spills, fines)
    );

    private static final Map<String, Double> E_WEIGHTS = Map.of(
        "carbon_emissions", 0.55, "renewable_energy_pct", 0.45
    );
    private static final Map<String, Double> S_WEIGHTS = Map.of(
        "employee_satisfaction", 0.5, "data_privacy_score", 0.5
    );
    private static final Map<String, Double> G_WEIGHTS = Map.of(
        "board_diversity", 0.5, "executive_pay_ratio", 0.5
    );

    @Override
    public void run(String... args) {
        long scorecardCount = scorecardRepository.count();
        long companyCount = companyRepository.findAll().size();
        log.info("Simulator starting — scorecards: {}, companies: {}", scorecardCount, companyCount);

        if (scorecardCount > 0) {
            log.info("Historical data already exists, skipping simulation.");
            return;
        }

        ScoringMethodology methodology = methodologyRepository.findByActive(true)
            .orElseThrow(() -> new IllegalStateException("No active methodology found"));

        List<Company> companies = companyRepository.findAll();
        log.info("Simulating {} months of GBM+jump history for {} companies...",
            MONTHS_OF_HISTORY, companies.size());

        for (Company company : companies) {
            simulateCompanyHistory(company, methodology);
        }

        log.info("Historical simulation complete.");
    }

    private void simulateCompanyHistory(Company company, ScoringMethodology methodology) {
        List<RawMetric> currentMetrics = rawMetricRepository.findByCompanyIdAndYear(company.getId(), 2024);
        if (currentMetrics.isEmpty()) return;

        Map<String, Double> metricMap = currentMetrics.stream()
            .collect(Collectors.toMap(RawMetric::getMetricName, RawMetric::getValue));

        // Compute current pillar scores — these are our "today" values
        double eNow = pillScore(metricMap, E_WEIGHTS);
        double sNow = pillScore(metricMap, S_WEIGHTS);
        double gNow = pillScore(metricMap, G_WEIGHTS);

        String sector = company.getSector();
        double[] gbm   = SECTOR_GBM.getOrDefault(sector,   new double[]{ 0.01, 0.01, 0.01, 0.04, 0.03, 0.03});
        double[] jumps = SECTOR_JUMPS.getOrDefault(sector, new double[]{ 0.5, 0.0, 4.0});

        // Work backwards from today to generate history
        // We reverse so the final point matches current seed data
        double[] eHistory = new double[MONTHS_OF_HISTORY];
        double[] sHistory = new double[MONTHS_OF_HISTORY];
        double[] gHistory = new double[MONTHS_OF_HISTORY];

        eHistory[MONTHS_OF_HISTORY - 1] = eNow;
        sHistory[MONTHS_OF_HISTORY - 1] = sNow;
        gHistory[MONTHS_OF_HISTORY - 1] = gNow;

        for (int i = MONTHS_OF_HISTORY - 2; i >= 0; i--) {
            eHistory[i] = stepBackward(eHistory[i + 1], gbm[0], gbm[3], jumps, DT);
            sHistory[i] = stepBackward(sHistory[i + 1], gbm[1], gbm[4], jumps, DT);
            gHistory[i] = stepBackward(gHistory[i + 1], gbm[2], gbm[5], jumps, DT);
        }

        // Save scorecards from oldest to newest
        for (int i = 0; i < MONTHS_OF_HISTORY; i++) {
            LocalDateTime timestamp = LocalDateTime.now()
                .minusMonths(MONTHS_OF_HISTORY - 1 - i)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0);

            double e = clamp(eHistory[i]);
            double s = clamp(sHistory[i]);
            double g = clamp(gHistory[i]);
            double composite = round(
                e * methodology.getEnvironmentalWeight() +
                s * methodology.getSocialWeight() +
                g * methodology.getGovernanceWeight()
            );

            ESGScorecard scorecard = new ESGScorecard();
            scorecard.setCompanyId(company.getId());
            scorecard.setMethodologyId(methodology.getId());
            scorecard.setEnvironmentalScore(round(e));
            scorecard.setSocialScore(round(s));
            scorecard.setGovernanceScore(round(g));
            scorecard.setCompositeScore(composite);
            scorecard.setTimestamp(timestamp);

            scorecardRepository.save(scorecard);
        }
    }

    /**
     * GBM with Merton jump diffusion, stepped backward in time.
     * S(t-dt) = S(t) / exp((mu - 0.5*sigma^2)*dt + sigma*sqrt(dt)*Z) - J
     * where J is a compound Poisson jump.
     */
    private double stepBackward(double current, double mu, double sigma, double[] jumpParams, double dt) {
        // GBM component
        double z = random.nextGaussian();
        double gbmFactor = Math.exp((mu - 0.5 * sigma * sigma) * dt + sigma * Math.sqrt(dt) * z);

        // Reverse the GBM step
        double prev = current / gbmFactor;

        // Jump component — Poisson arrival
        double lambda    = jumpParams[0];
        double muJump    = jumpParams[1];
        double sigmaJump = jumpParams[2];

        // Expected number of jumps in this interval
        double expectedJumps = lambda * dt;
        if (random.nextDouble() < expectedJumps) {
            // A jump occurred — reverse it by subtracting
            double jumpSize = muJump + sigmaJump * random.nextGaussian();
            prev -= jumpSize; // subtract because we're going backward
        }

        return prev;
    }

    private double pillScore(Map<String, Double> metrics, Map<String, Double> weights) {
        double score = 0.0;
        double totalWeight = 0.0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            if (metrics.containsKey(entry.getKey())) {
                score += metrics.get(entry.getKey()) * entry.getValue();
                totalWeight += entry.getValue();
            }
        }
        return totalWeight > 0 ? score / totalWeight : 0.0;
    }

    private double clamp(double value) {
        return Math.max(1.0, Math.min(99.0, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}