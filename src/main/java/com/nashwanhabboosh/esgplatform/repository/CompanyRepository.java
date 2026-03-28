package com.nashwanhabboosh.esgplatform.repository;

import com.nashwanhabboosh.esgplatform.model.Company;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends MongoRepository<Company, String> {
    Optional<Company> findByTicker(String ticker);
    List<Company> findBySector(String sector);
}