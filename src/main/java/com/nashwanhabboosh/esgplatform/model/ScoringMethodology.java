package com.nashwanhabboosh.esgplatform.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "scoring_methodologies")
public class ScoringMethodology {
    @Id
    private String id;
    private String name;
    private String description;
    private boolean active;
    private double environmentalWeight;
    private double socialWeight;
    private double governanceWeight;
}