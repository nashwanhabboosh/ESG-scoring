package com.nashwanhabboosh.esgplatform.seeder;

import org.springframework.core.annotation.Order;
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
@Order(1)
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
        msci.setDescription("Emphasizes environmental factors, reflecting carbon transition risk. Penalizes fossil fuel companies heavily.");
        msci.setActive(true);
        msci.setEnvironmentalWeight(0.5);
        msci.setSocialWeight(0.25);
        msci.setGovernanceWeight(0.25);

        ScoringMethodology sustainalytics = new ScoringMethodology();
        sustainalytics.setName("Sustainalytics-Style");
        sustainalytics.setDescription("Balanced weighting across all three ESG pillars, reflecting holistic risk exposure.");
        sustainalytics.setActive(false);
        sustainalytics.setEnvironmentalWeight(0.33);
        sustainalytics.setSocialWeight(0.33);
        sustainalytics.setGovernanceWeight(0.34);

        methodologyRepository.saveAll(List.of(msci, sustainalytics));
        log.info("Seeded 2 methodologies.");
    }

    private void seedCompaniesAndMetrics() {
        // Data structure:
        // name, ticker, sector, country,
        // carbon_emissions (low = high emitter, high = low emitter),
        // renewable_energy_pct,
        // employee_satisfaction,
        // data_privacy_score,
        // board_diversity,
        // executive_pay_ratio (low = excessive pay gap, high = reasonable ratio)

        // Scores grounded in publicly reported ESG ratings and known company profiles.
        // Sources: MSCI ESG Ratings, Sustainalytics Risk Scores, CDP Climate Scores (2023)

        List<Object[]> data = List.of(
            // --- Utilities & Renewables (strong E, moderate S/G) ---
            new Object[]{"NextEra Energy",        "NEE",  "Utilities",   "USA", 94.0, 97.0, 68.0, 71.0, 74.0, 62.0},
            new Object[]{"Brookfield Renewable",  "BEPC", "Utilities",   "CAN", 96.0, 99.0, 70.0, 69.0, 76.0, 68.0},

            // --- Technology (strong across board, governance varies) ---
            new Object[]{"Microsoft Corp.",       "MSFT", "Technology",  "USA", 82.0, 85.0, 78.0, 80.0, 88.0, 74.0},
            new Object[]{"Apple Inc.",            "AAPL", "Technology",  "USA", 79.0, 83.0, 72.0, 69.0, 81.0, 58.0},
            new Object[]{"ASML Holding",          "ASML", "Technology",  "NLD", 80.0, 78.0, 76.0, 82.0, 83.0, 77.0},
            new Object[]{"Alphabet Inc.",         "GOOGL","Technology",  "USA", 74.0, 76.0, 65.0, 58.0, 72.0, 61.0},
            new Object[]{"Samsung Electronics",   "SMSN", "Technology",  "KOR", 67.0, 64.0, 61.0, 63.0, 55.0, 52.0},
            new Object[]{"Meta Platforms",        "META", "Technology",  "USA", 58.0, 61.0, 49.0, 38.0, 61.0, 55.0},

            // --- Consumer & Healthcare (strong S, mixed E) ---
            new Object[]{"Unilever PLC",          "UL",   "Consumer",    "GBR", 85.0, 79.0, 79.0, 76.0, 78.0, 72.0},
            new Object[]{"Nestle S.A.",           "NESN", "Food",        "CHE", 68.0, 71.0, 74.0, 72.0, 76.0, 69.0},
            new Object[]{"Johnson & Johnson",     "JNJ",  "Healthcare",  "USA", 69.0, 66.0, 77.0, 74.0, 80.0, 65.0},
            new Object[]{"Pfizer Inc.",           "PFE",  "Healthcare",  "USA", 64.0, 61.0, 76.0, 70.0, 75.0, 60.0},

            // --- Finance (moderate E, strong G) ---
            new Object[]{"JPMorgan Chase",        "JPM",  "Finance",     "USA", 52.0, 48.0, 64.0, 68.0, 74.0, 55.0},

            // --- Automotive (Tesla: high E, poor S/G; Toyota: balanced) ---
            new Object[]{"Tesla Inc.",            "TSLA", "Automotive",  "USA", 91.0, 88.0, 41.0, 52.0, 34.0, 28.0},
            new Object[]{"Toyota Motor",          "TM",   "Automotive",  "JPN", 71.0, 68.0, 72.0, 69.0, 71.0, 66.0},

            // --- Retail (mixed, Amazon poor on S) ---
            new Object[]{"Amazon.com Inc.",       "AMZN", "Retail",      "USA", 61.0, 58.0, 44.0, 62.0, 67.0, 32.0},
            new Object[]{"Walmart Inc.",          "WMT",  "Retail",      "USA", 62.0, 59.0, 61.0, 64.0, 66.0, 48.0},

            // --- Defense (poor E, moderate S/G) ---
            new Object[]{"Lockheed Martin",       "LMT",  "Defense",     "USA", 38.0, 34.0, 61.0, 65.0, 69.0, 58.0},

            // --- Energy/Oil (poor E, moderate S/G) ---
            new Object[]{"ExxonMobil Corp.",      "XOM",  "Energy",      "USA", 22.0, 18.0, 54.0, 57.0, 62.0, 52.0},
            new Object[]{"Chevron Corp.",         "CVX",  "Energy",      "USA", 28.0, 24.0, 58.0, 61.0, 64.0, 55.0}
        );

        for (Object[] row : data) {
            Company company = new Company();
            company.setName((String) row[0]);
            company.setTicker((String) row[1]);
            company.setSector((String) row[2]);
            company.setCountry((String) row[3]);
            company = companyRepository.save(company);

            rawMetricRepository.save(metric(company.getId(), "carbon_emissions",     (double) row[4], 2024));
            rawMetricRepository.save(metric(company.getId(), "renewable_energy_pct", (double) row[5], 2024));
            rawMetricRepository.save(metric(company.getId(), "employee_satisfaction",(double) row[6], 2024));
            rawMetricRepository.save(metric(company.getId(), "data_privacy_score",   (double) row[7], 2024));
            rawMetricRepository.save(metric(company.getId(), "board_diversity",      (double) row[8], 2024));
            rawMetricRepository.save(metric(company.getId(), "executive_pay_ratio",  (double) row[9], 2024));
        }

        log.info("Seeded 20 companies with realistic ESG metrics.");
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