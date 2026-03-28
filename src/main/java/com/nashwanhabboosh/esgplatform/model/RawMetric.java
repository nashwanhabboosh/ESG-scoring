package com.nashwanhabboosh.esgplatform.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "raw_metrics")
public class RawMetric {
    @Id
    private String id;
    private String companyId;
    private String metricName;
    private double value;
    private int year;
}