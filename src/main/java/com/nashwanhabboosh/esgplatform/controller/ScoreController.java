package com.nashwanhabboosh.esgplatform.controller;

import com.nashwanhabboosh.esgplatform.model.ESGScorecard;
import com.nashwanhabboosh.esgplatform.repository.ESGScorecardRepository;
import com.nashwanhabboosh.esgplatform.service.ScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoringService scoringService;
    private final ESGScorecardRepository scorecardRepository;

    @GetMapping("/company/{companyId}")
    public ResponseEntity<ESGScorecard> getLatestScore(@PathVariable String companyId) {
        return scorecardRepository.findTopByCompanyIdOrderByTimestampDesc(companyId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/company/{companyId}")
    public ESGScorecard scoreCompany(@PathVariable String companyId) {
        return scoringService.scoreCompany(companyId);
    }

    @GetMapping("/company/{companyID}/history")
    public List<ESGScorecard> getScoreHistory(@PathVariable String companyID) {
        return scorecardRepository.findByCompanyIdOrderByTimestampDesc(companyID);
    }

    @PostMapping("/score-all")
    public List<ESGScorecard> scoreAll() {
        return scoringService.scoreAllCompanies();
    }

    @GetMapping("/leaderboard")
    public List<ESGScorecard> getLeaderboard() {
        return scorecardRepository.findAll()
            .stream()
            .collect(Collectors.toMap(
                ESGScorecard::getCompanyId,
                s -> s,
                (s1, s2) -> s1.getTimestamp().isAfter(s2.getTimestamp()) ? s1 : s2
            ))
            .values()
            .stream()
            .sorted(Comparator.comparingDouble(ESGScorecard::getCompositeScore).reversed())
            .collect(Collectors.toList());
    }

    @GetMapping("/compare")
    public List<ESGScorecard> compareCompanies(@RequestParam List<String> ids) {
        return ids.stream()
            .map(id -> scorecardRepository.findTopByCompanyIdOrderByTimestampDesc(id))
            .filter(java.util.Optional::isPresent)
            .map(java.util.Optional::get)
            .collect(Collectors.toList());
    }
}