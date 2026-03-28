package com.nashwanhabboosh.esgplatform.seeder;

import com.nashwanhabboosh.esgplatform.model.Company;
import com.nashwanhabboosh.esgplatform.model.RawMetric;
import com.nashwanhabboosh.esgplatform.model.ScoringMethodology;
import com.nashwanhabboosh.esgplatform.repository.CompanyRepository;
import com.nashwanhabboosh.esgplatform.repository.RawMetricRepository;
import com.nashwanhabboosh.esgplatform.repository.ScoringMethodologyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final RawMetricRepository rawMetricRepository;
    private final ScoringMethodologyRepository methodologyRepository;

    @Override
    public void run(String... args) {
        if (companyRepository.count() > 0) {
            log.info("Database already seeded, skipping.");
            return;
        }

        log.info("Seeding database...");
        seedMethodologies();
        seedCompaniesAndMetrics();
        log.info("Seeding complete.");
    }

    private void seedMethodologies() {
        ScoringMethodology msci = new ScoringMethodology();
        msci.setName("MSCI-Style");
        msci.setDescription("Emphasizes environmental factors, reflecting carbon transition risk");
        msci.setActive(true);
        msci.setEnvironmentalWeight(0.5);
        msci.setSocialWeight(0.25);
        msci.setGovernanceWeight(0.25);

        ScoringMethodology sustainalytics = new ScoringMethodology();
        sustainalytics.setName("Sustainalytics-Style");
        sustainalytics.setDescription("Balanced weighting across all three ESG pillars");
        sustainalytics.setActive(false);
        sustainalytics.setEnvironmentalWeight(0.33);
        sustainalytics.setSocialWeight(0.33);
        sustainalytics.setGovernanceWeight(0.34);

        methodologyRepository.saveAll(List.of(msci, sustainalytics));
        log.info("Seeded 2 methodologies.");
    }

    private void seedCompaniesAndMetrics() {
        List<Object[]> data = List.of(
            new Object[]{"Apple Inc.",        "AAPL", "Technology",  "USA", 78.0, 72.0, 81.0},
            new Object[]{"Microsoft Corp.",   "MSFT", "Technology",  "USA", 82.0, 75.0, 88.0},
            new Object[]{"Tesla Inc.",        "TSLA", "Automotive",  "USA", 91.0, 55.0, 49.0},
            new Object[]{"ExxonMobil Corp.",  "XOM",  "Energy",      "USA", 31.0, 58.0, 62.0},
            new Object[]{"JPMorgan Chase",    "JPM",  "Finance",     "USA", 54.0, 66.0, 74.0},
            new Object[]{"Amazon.com Inc.",   "AMZN", "Retail",      "USA", 61.0, 52.0, 69.0},
            new Object[]{"Alphabet Inc.",     "GOOGL","Technology",  "USA", 74.0, 68.0, 72.0},
            new Object[]{"Chevron Corp.",     "CVX",  "Energy",      "USA", 34.0, 61.0, 65.0},
            new Object[]{"Johnson & Johnson", "JNJ",  "Healthcare",  "USA", 69.0, 77.0, 80.0},
            new Object[]{"Walmart Inc.",      "WMT",  "Retail",      "USA", 64.0, 70.0, 66.0},
            new Object[]{"Nestle S.A.",       "NESN", "Food",        "CHE", 72.0, 74.0, 78.0},
            new Object[]{"ASML Holding",      "ASML", "Technology",  "NLD", 80.0, 76.0, 83.0},
            new Object[]{"Unilever PLC",      "UL",   "Consumer",    "GBR", 85.0, 79.0, 75.0},
            new Object[]{"Toyota Motor",      "TM",   "Automotive",  "JPN", 76.0, 71.0, 73.0},
            new Object[]{"Samsung Electronics","SMSN","Technology",  "KOR", 70.0, 65.0, 68.0},
            new Object[]{"Meta Platforms",    "META", "Technology",  "USA", 58.0, 49.0, 61.0},
            new Object[]{"Pfizer Inc.",       "PFE",  "Healthcare",  "USA", 66.0, 80.0, 76.0},
            new Object[]{"Lockheed Martin",   "LMT",  "Defense",     "USA", 42.0, 63.0, 71.0},
            new Object[]{"NextEra Energy",    "NEE",  "Utilities",   "USA", 94.0, 68.0, 72.0},
            new Object[]{"Brookfield Renewable","BEPC","Utilities",  "CAN", 96.0, 70.0, 74.0}
        );

        for (Object[] row : data) {
            Company company = new Company();
            company.setName((String) row[0]);
            company.setTicker((String) row[1]);
            company.setSector((String) row[2]);
            company.setCountry((String) row[3]);
            company = companyRepository.save(company);

            String[] metricNames = {"carbon_emissions", "renewable_energy_pct", "board_diversity",
                                    "employee_satisfaction", "data_privacy_score", "executive_pay_ratio"};
            double[] eScores = {(double) row[4], (double) row[4] * 0.95};
            double[] sScores = {(double) row[5], (double) row[5] * 0.98};
            double[] gScores = {(double) row[6], (double) row[6] * 1.02};

            rawMetricRepository.save(metric(company.getId(), "carbon_emissions",      eScores[0], 2024));
            rawMetricRepository.save(metric(company.getId(), "renewable_energy_pct",  eScores[1], 2024));
            rawMetricRepository.save(metric(company.getId(), "employee_satisfaction", sScores[0], 2024));
            rawMetricRepository.save(metric(company.getId(), "data_privacy_score",    sScores[1], 2024));
            rawMetricRepository.save(metric(company.getId(), "board_diversity",       gScores[0], 2024));
            rawMetricRepository.save(metric(company.getId(), "executive_pay_ratio",   gScores[1], 2024));
        }

        log.info("Seeded 20 companies with metrics.");
    }

    private RawMetric metric(String companyId, String name, double value, int year) {
        RawMetric m = new RawMetric();
        m.setCompanyId(companyId);
        m.setMetricName(name);
        m.setValue(value);
        m.setYear(year);
        return m;
    }
}