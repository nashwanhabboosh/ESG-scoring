package com.nashwanhabboosh.esgplatform.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "esg_scorecards")
public class ESGScorecard {
    @Id
    private String id;
    private String companyId;
    private String methodologyId;
    private double environmentalScore;
    private double socialScore;
    private double governanceScore;
    private double compositeScore;
    private LocalDateTime timestamp;
}