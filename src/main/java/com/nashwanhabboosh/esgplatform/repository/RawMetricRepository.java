package com.nashwanhabboosh.esgplatform.repository;

import com.nashwanhabboosh.esgplatform.model.RawMetric;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RawMetricRepository extends MongoRepository<RawMetric, String> {
    List<RawMetric> findByCompanyId(String companyId);
    List<RawMetric> findByCompanyIdAndYear(String companyId, int year);
}