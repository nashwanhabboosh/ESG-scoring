package com.nashwanhabboosh.esgplatform.service;

import com.nashwanhabboosh.esgplatform.dto.SectorSummary;
import com.nashwanhabboosh.esgplatform.model.Company;
import com.nashwanhabboosh.esgplatform.repository.CompanyRepository;
import com.nashwanhabboosh.esgplatform.repository.ESGScorecardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorAnalyticsService {

    private final CompanyRepository companyRepository;
    private final ESGScorecardRepository scorecardRepository;

    public List<SectorSummary> getAllSectorSummaries() {
        List<Company> companies = companyRepository.findAll();

        Map<String, List<Company>> bySector = companies.stream()
            .collect(Collectors.groupingBy(Company::getSector));

        List<SectorSummary> summaries = new ArrayList<>();

        for (Map.Entry<String, List<Company>> entry : bySector.entrySet()) {
            SectorSummary summary = buildSectorSummary(entry.getKey(), entry.getValue());
            if (summary != null) summaries.add(summary);
        }

        return summaries.stream()
            .sorted(Comparator.comparingDouble(SectorSummary::getAvgCompositeScore).reversed())
            .collect(Collectors.toList());
    }

    public SectorSummary getSectorDetail(String sector) {
        List<Company> companies = companyRepository.findAll().stream()
            .filter(c -> sector.equalsIgnoreCase(c.getSector()))
            .collect(Collectors.toList());

        return buildSectorSummary(sector, companies);
    }

    private SectorSummary buildSectorSummary(String sector, List<Company> companies) {
        List<SectorSummary.CompanyScore> companyScores = new ArrayList<>();

        for (Company company : companies) {
            scorecardRepository.findTopByCompanyIdOrderByTimestampDesc(company.getId())
                .ifPresent(sc -> companyScores.add(new SectorSummary.CompanyScore(
                    company.getId(),
                    company.getName(),
                    company.getTicker(),
                    round(sc.getEnvironmentalScore()),
                    round(sc.getSocialScore()),
                    round(sc.getGovernanceScore()),
                    round(sc.getCompositeScore())
                )));
        }

        if (companyScores.isEmpty()) return null;

        double avgE = round(companyScores.stream().mapToDouble(SectorSummary.CompanyScore::getEnvironmentalScore).average().orElse(0));
        double avgS = round(companyScores.stream().mapToDouble(SectorSummary.CompanyScore::getSocialScore).average().orElse(0));
        double avgG = round(companyScores.stream().mapToDouble(SectorSummary.CompanyScore::getGovernanceScore).average().orElse(0));
        double avgC = round(companyScores.stream().mapToDouble(SectorSummary.CompanyScore::getCompositeScore).average().orElse(0));

        companyScores.sort(Comparator.comparingDouble(SectorSummary.CompanyScore::getCompositeScore).reversed());

        return new SectorSummary(sector, avgE, avgS, avgG, avgC, companies.size(), companyScores);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}