package com.nashwanhabboosh.esgplatform.repository;

import com.nashwanhabboosh.esgplatform.model.ESGScorecard;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ESGScorecardRepository extends MongoRepository<ESGScorecard, String> {
    List<ESGScorecard> findByCompanyIdOrderByTimestampDesc(String companyId);
    Optional<ESGScorecard> findTopByCompanyIdOrderByTimestampDesc(String companyId);
    List<ESGScorecard> findByMethodologyId(String methodologyId);
}