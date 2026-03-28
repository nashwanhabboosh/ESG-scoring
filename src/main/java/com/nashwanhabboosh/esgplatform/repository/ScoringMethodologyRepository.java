package com.nashwanhabboosh.esgplatform.repository;

import com.nashwanhabboosh.esgplatform.model.ScoringMethodology;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ScoringMethodologyRepository extends MongoRepository<ScoringMethodology, String> {
    Optional<ScoringMethodology> findByActive(boolean active);
    Optional<ScoringMethodology> findByName(String name);
}