package com.nashwanhabboosh.esgplatform;

import com.nashwanhabboosh.esgplatform.model.ESGScorecard;
import com.nashwanhabboosh.esgplatform.model.RawMetric;
import com.nashwanhabboosh.esgplatform.model.ScoringMethodology;
import com.nashwanhabboosh.esgplatform.repository.ESGScorecardRepository;
import com.nashwanhabboosh.esgplatform.repository.RawMetricRepository;
import com.nashwanhabboosh.esgplatform.repository.ScoringMethodologyRepository;
import com.nashwanhabboosh.esgplatform.service.ScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock private RawMetricRepository rawMetricRepository;
    @Mock private ESGScorecardRepository scorecardRepository;
    @Mock private ScoringMethodologyRepository methodologyRepository;

    @InjectMocks private ScoringService scoringService;

    private ScoringMethodology msciMethodology;
    private ScoringMethodology balancedMethodology;
    private List<RawMetric> fullMetrics;

    @BeforeEach
    void setUp() {
        msciMethodology = new ScoringMethodology();
        msciMethodology.setId("method-1");
        msciMethodology.setName("MSCI-Style");
        msciMethodology.setActive(true);
        msciMethodology.setEnvironmentalWeight(0.5);
        msciMethodology.setSocialWeight(0.25);
        msciMethodology.setGovernanceWeight(0.25);

        balancedMethodology = new ScoringMethodology();
        balancedMethodology.setId("method-2");
        balancedMethodology.setName("Balanced");
        balancedMethodology.setActive(false);
        balancedMethodology.setEnvironmentalWeight(0.33);
        balancedMethodology.setSocialWeight(0.33);
        balancedMethodology.setGovernanceWeight(0.34);

        fullMetrics = List.of(
            metric("company-1", "carbon_emissions",     80.0),
            metric("company-1", "renewable_energy_pct", 70.0),
            metric("company-1", "employee_satisfaction",60.0),
            metric("company-1", "data_privacy_score",   50.0),
            metric("company-1", "board_diversity",       90.0),
            metric("company-1", "executive_pay_ratio",   80.0)
        );
    }

    @Test
    void scoreCompany_usesActiveMethodology() {
        when(methodologyRepository.findByActive(true)).thenReturn(Optional.of(msciMethodology));
        when(rawMetricRepository.findByCompanyIdAndYear("company-1", 2024)).thenReturn(fullMetrics);
        when(scorecardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ESGScorecard result = scoringService.scoreCompany("company-1");

        assertNotNull(result);
        assertEquals("company-1", result.getCompanyId());
        assertEquals("method-1", result.getMethodologyId());
        verify(methodologyRepository).findByActive(true);
    }

    @Test
    void scoreCompany_compositeIsWeightedCorrectly() {
        when(methodologyRepository.findByActive(true)).thenReturn(Optional.of(msciMethodology));
        when(rawMetricRepository.findByCompanyIdAndYear("company-1", 2024)).thenReturn(fullMetrics);
        when(scorecardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ESGScorecard result = scoringService.scoreCompany("company-1");

        double expectedComposite = (result.getEnvironmentalScore() * 0.5)
                                 + (result.getSocialScore() * 0.25)
                                 + (result.getGovernanceScore() * 0.25);

        assertEquals(Math.round(expectedComposite * 100.0) / 100.0, result.getCompositeScore());
    }

    @Test
    void scoreCompany_differentMethodologiesProduceDifferentScores() {
        when(rawMetricRepository.findByCompanyIdAndYear("company-1", 2024)).thenReturn(fullMetrics);
        when(scorecardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ESGScorecard msciResult = scoringService.scoreCompany("company-1", msciMethodology);
        ESGScorecard balancedResult = scoringService.scoreCompany("company-1", balancedMethodology);

        assertNotEquals(msciResult.getCompositeScore(), balancedResult.getCompositeScore(),
            "Different methodologies should produce different composite scores");
    }

    @Test
    void scoreCompany_throwsWhenNoMetricsFound() {
        when(methodologyRepository.findByActive(true)).thenReturn(Optional.of(msciMethodology));
        when(rawMetricRepository.findByCompanyIdAndYear("company-1", 2024)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> scoringService.scoreCompany("company-1"));
    }

    @Test
    void scoreCompany_throwsWhenNoActiveMethodology() {
        when(methodologyRepository.findByActive(true)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> scoringService.scoreCompany("company-1"));
    }

    @Test
    void scoreCompany_handlesPartialMetrics() {
        List<RawMetric> partialMetrics = List.of(
            metric("company-1", "carbon_emissions", 80.0),
            metric("company-1", "employee_satisfaction", 60.0),
            metric("company-1", "board_diversity", 90.0)
        );

        when(methodologyRepository.findByActive(true)).thenReturn(Optional.of(msciMethodology));
        when(rawMetricRepository.findByCompanyIdAndYear("company-1", 2024)).thenReturn(partialMetrics);
        when(scorecardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ESGScorecard result = scoringService.scoreCompany("company-1");

        assertNotNull(result);
        assertTrue(result.getEnvironmentalScore() > 0);
        assertTrue(result.getCompositeScore() > 0);
    }

    @Test
    void scoreAllCompanies_scoresEachDistinctCompany() {
        List<RawMetric> multiCompanyMetrics = List.of(
            metric("company-1", "carbon_emissions", 80.0),
            metric("company-1", "renewable_energy_pct", 70.0),
            metric("company-1", "employee_satisfaction", 60.0),
            metric("company-1", "data_privacy_score", 50.0),
            metric("company-1", "board_diversity", 90.0),
            metric("company-1", "executive_pay_ratio", 80.0),
            metric("company-2", "carbon_emissions", 50.0),
            metric("company-2", "renewable_energy_pct", 40.0),
            metric("company-2", "employee_satisfaction", 70.0),
            metric("company-2", "data_privacy_score", 60.0),
            metric("company-2", "board_diversity", 55.0),
            metric("company-2", "executive_pay_ratio", 65.0)
        );

        when(methodologyRepository.findByActive(true)).thenReturn(Optional.of(msciMethodology));
        when(rawMetricRepository.findAll()).thenReturn(multiCompanyMetrics);
        when(rawMetricRepository.findByCompanyIdAndYear(eq("company-1"), eq(2024)))
            .thenReturn(multiCompanyMetrics.subList(0, 6));
        when(rawMetricRepository.findByCompanyIdAndYear(eq("company-2"), eq(2024)))
            .thenReturn(multiCompanyMetrics.subList(6, 12));
        when(scorecardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<ESGScorecard> results = scoringService.scoreAllCompanies();

        assertEquals(2, results.size());
        verify(scorecardRepository, times(2)).save(any());
    }

    private RawMetric metric(String companyId, String name, double value) {
        RawMetric m = new RawMetric();
        m.setCompanyId(companyId);
        m.setMetricName(name);
        m.setValue(value);
        m.setYear(2024);
        return m;
    }
}