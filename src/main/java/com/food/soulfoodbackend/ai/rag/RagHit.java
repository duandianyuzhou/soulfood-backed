package com.food.soulfoodbackend.ai.rag;

import lombok.Data;

@Data
public class RagHit {

    private Long id;
    private String sourceType;
    private Long sourceId;
    private String sourceKey;
    private String title;
    private String content;
    private Double distance;
}
