package com.food.soulfoodbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiRagProperties {

    private String embeddingModel = "qwen3-embedding:0.6b";
    private int embeddingDims = 1024;
    private Rag rag = new Rag();
    private int toolMaxCalls = 5;
    private int toolTimeoutMs = 8000;

    @Data
    public static class Rag {
        /**
         * lexical：只关键词
         * embedding：只向量
         * hybrid / auto：关键词 + 向量 RRF；向量失败则退回关键词
         */
        private String mode = "lexical";
        private boolean ingestOnStartup = true;
        private int topK = 5;
    }
}
