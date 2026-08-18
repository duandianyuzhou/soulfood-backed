package com.food.soulfoodbackend.ai.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagSeedItem {

    private String sourceType;
    private String sourceKey;
    private String title;
    private String content;
}
