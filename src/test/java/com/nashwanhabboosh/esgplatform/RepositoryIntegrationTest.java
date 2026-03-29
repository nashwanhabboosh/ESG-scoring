package com.nashwanhabboosh.esgplatform;

import com.nashwanhabboosh.esgplatform.model.Company;
import com.nashwanhabboosh.esgplatform.model.ESGScorecard;
import com.nashwanhabboosh.esgplatform.model.RawMetric;
import com.nashwanhabboosh.esgplatform.model.ScoringMethodology;
import com.nashwanhabboosh.esgplatform.repository.CompanyRepository;
import com.nashwanhabboosh.esgplatform.repository.ESGScorecardRepository;
import com.nashwanhabboosh.esgplatform.repository.RawMetricRepository;
import com.nashwanhabboosh.esgplatform.repository.ScoringMethodologyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RepositoryIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private RawMetricRepository rawMetricRepository;
    @Autowired private ESGScorecardRepository scorecardRepository;
    @Autowired private ScoringMethodologyRepository methodologyRepository;

    @BeforeEach
    void setUp() {
        companyRepository.deleteAll();
        rawMetricRepository.deleteAll();
        scorecardRepository.deleteAll();
        methodologyRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        companyRepository.deleteAll();
        rawMetricRepository.deleteAll();
        scorecardRepository.deleteAll();
        methodologyRepository.deleteAll();
    }

    @Test
    void company_saveAndFindById() {
        Company company = new Company();
        company.setName("Test Corp");
        company.setTicker("TEST");
        company.setSector("Technology");
        company.setCountry("USA");

        Company saved = companyRepository.save(company);
        Optional<Company> found = companyRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Test Corp", found.get().getName());
        assertEquals("TEST", found.get().getTicker());
    }

    @Test
    void company_findByTicker() {
        Company company = new Company();
        company.setName("Apple Inc.");
        company.setTicker("AAPL");
        company.setSector("Technology");
        company.setCountry("USA");
        companyRepository.save(company);

        Optional<Company> found = companyRepository.findByTicker("AAPL");

        assertTrue(found.isPresent());
        assertEquals("Apple Inc.", found.get().getName());
    }

    @Test
    void company_findBySector() {
        companyRepository.saveAll(List.of(
            company("Microsoft", "MSFT", "Technology"),
            company("Google", "GOOGL", "Technology"),
            company("ExxonMobil", "XOM", "Energy")
        ));

        List<Company> techCompanies = companyRepository.findBySector("Technology");

        assertEquals(2, techCompanies.size());
    }

    @Test
    void rawMetric_findByCompanyIdAndYear() {
        rawMetricRepository.saveAll(List.of(
            metric("company-1", "carbon_emissions", 80.0, 2024),
            metric("company-1", "board_diversity", 70.0, 2024),
            metric("company-1", "carbon_emissions", 75.0, 2023)
        ));

        List<RawMetric> results = rawMetricRepository.findByCompanyIdAndYear("company-1", 2024);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(m -> m.getYear() == 2024));
    }

    @Test
    void methodology_findByActive() {
        methodologyRepository.saveAll(List.of(
            methodology("MSCI-Style", true),
            methodology("Balanced", false)
        ));

        Optional<ScoringMethodology> found = methodologyRepository.findByActive(true);

        assertTrue(found.isPresent());
        assertEquals("MSCI-Style", found.get().getName());
    }

    @Test
    void scorecard_findTopByCompanyIdOrderByTimestampDesc() {
        scorecardRepository.saveAll(List.of(
            scorecard("company-1", 70.0, LocalDateTime.now().minusDays(1)),
            scorecard("company-1", 85.0, LocalDateTime.now())
        ));

        Optional<ESGScorecard> latest = scorecardRepository
            .findTopByCompanyIdOrderByTimestampDesc("company-1");

        assertTrue(latest.isPresent());
        assertEquals(85.0, latest.get().getCompositeScore());
    }

    @Test
    void scorecard_findByCompanyIdOrderByTimestampDesc_returnsInOrder() {
        scorecardRepository.saveAll(List.of(
            scorecard("company-1", 60.0, LocalDateTime.now().minusDays(2)),
            scorecard("company-1", 70.0, LocalDateTime.now().minusDays(1)),
            scorecard("company-1", 80.0, LocalDateTime.now())
        ));

        List<ESGScorecard> results = scorecardRepository
            .findByCompanyIdOrderByTimestampDesc("company-1");

        assertEquals(3, results.size());
        assertEquals(80.0, results.get(0).getCompositeScore());
        assertEquals(60.0, results.get(2).getCompositeScore());
    }

    private Company company(String name, String ticker, String sector) {
        Company c = new Company();
        c.setName(name);
        c.setTicker(ticker);
        c.setSector(sector);
        c.setCountry("USA");
        return c;
    }

    private RawMetric metric(String companyId, String name, double value, int year) {
        RawMetric m = new RawMetric();
        m.setCompanyId(companyId);
        m.setMetricName(name);
        m.setValue(value);
        m.setYear(year);
        return m;
    }

    private ScoringMethodology methodology(String name, boolean active) {
        ScoringMethodology m = new ScoringMethodology();
        m.setName(name);
        m.setActive(active);
        m.setEnvironmentalWeight(0.5);
        m.setSocialWeight(0.25);
        m.setGovernanceWeight(0.25);
        return m;
    }

    private ESGScorecard scorecard(String companyId, double composite, LocalDateTime timestamp) {
        ESGScorecard s = new ESGScorecard();
        s.setCompanyId(companyId);
        s.setCompositeScore(composite);
        s.setEnvironmentalScore(composite);
        s.setSocialScore(composite);
        s.setGovernanceScore(composite);
        s.setTimestamp(timestamp);
        return s;
    }
}