package com.nashwanhabboosh.esgplatform;

import com.nashwanhabboosh.esgplatform.model.Company;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();
    private String authHeader;

    @Autowired private CompanyRepository companyRepository;
    @Autowired private RawMetricRepository rawMetricRepository;
    @Autowired private ESGScorecardRepository scorecardRepository;
    @Autowired private ScoringMethodologyRepository methodologyRepository;

    @BeforeEach
    void setUp() {
        String credentials = Base64.getEncoder().encodeToString("admin:esg-admin-2024".getBytes());
        authHeader = "Basic " + credentials;

        // Clean before each test to remove any seeded data
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

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    @Test
    void getCompanies_returnsEmptyList() {
        ResponseEntity<List> response = restTemplate.getForEntity(url("/api/companies"), List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getCompanies_returnsSeededCompanies() {
        Company c = new Company();
        c.setName("Apple Inc.");
        c.setTicker("AAPL");
        c.setSector("Technology");
        c.setCountry("USA");
        companyRepository.save(c);

        ResponseEntity<List> response = restTemplate.getForEntity(url("/api/companies"), List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createCompany_requiresAuth() {
        Company c = new Company();
        c.setName("Test Corp");
        c.setTicker("TEST");
        c.setSector("Technology");
        c.setCountry("USA");

        try {
            restTemplate.postForEntity(url("/api/companies"), c, Company.class);
            fail("Expected 401");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    void createCompany_withAuth_succeeds() {
        Company c = new Company();
        c.setName("Test Corp");
        c.setTicker("TEST");
        c.setSector("Technology");
        c.setCountry("USA");

        HttpEntity<Company> request = new HttpEntity<>(c, authHeaders());
        ResponseEntity<Company> response = restTemplate.exchange(
            url("/api/companies"), HttpMethod.POST, request, Company.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getId());
        assertEquals("TEST", response.getBody().getTicker());
    }

    @Test
    void getCompany_notFound_returns404() {
        try {
            restTemplate.getForEntity(url("/api/companies/nonexistent-id"), Company.class);
            fail("Expected 404");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    void getMethodologies_returnsAll() {
        methodologyRepository.save(methodology("MSCI-Style", true));

        ResponseEntity<List> response = restTemplate.getForEntity(url("/api/methodologies"), List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createMethodology_withAuth_isInactiveByDefault() {
        ScoringMethodology m = methodology("Custom", false);
        HttpEntity<ScoringMethodology> request = new HttpEntity<>(m, authHeaders());
        ResponseEntity<ScoringMethodology> response = restTemplate.exchange(
            url("/api/methodologies"), HttpMethod.POST, request, ScoringMethodology.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isActive());
    }

    @Test
    void activateMethodology_deactivatesOthers() {
        methodologyRepository.save(methodology("MSCI-Style", true));
        ScoringMethodology m2 = methodologyRepository.save(methodology("Balanced", false));

        HttpEntity<Void> request = new HttpEntity<>(authHeaders());
        ResponseEntity<ScoringMethodology> response = restTemplate.exchange(
            url("/api/methodologies/" + m2.getId() + "/activate"),
            HttpMethod.PUT, request, ScoringMethodology.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isActive());
    }

    @Test
    void getLeaderboard_returnsEmpty_whenNoScores() {
        ResponseEntity<List> response = restTemplate.getForEntity(url("/api/scores/leaderboard"), List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getScore_notFound_returns404() {
        try {
            restTemplate.getForEntity(url("/api/scores/company/nonexistent-id"), Object.class);
            fail("Expected 404");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
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
}