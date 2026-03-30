package com.nashwanhabboosh.esgplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SectorSummary {
    private String sector;
    private double avgEnvironmentalScore;
    private double avgSocialScore;
    private double avgGovernanceScore;
    private double avgCompositeScore;
    private int companyCount;
    private List<CompanyScore> companies;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompanyScore {
        private String companyId;
        private String name;
        private String ticker;
        private double environmentalScore;
        private double socialScore;
        private double governanceScore;
        private double compositeScore;
    }
}