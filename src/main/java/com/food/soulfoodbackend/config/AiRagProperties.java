package com.food.soulfoodbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiRagProperties {

    private String embeddingModel = "embedding-3";
    private int embeddingDims = 1024;
    private Rag rag = new Rag();

    @Data
    public static class Rag {
        private boolean ingestOnStartup = true;
        private int topK = 5;
    }
}
