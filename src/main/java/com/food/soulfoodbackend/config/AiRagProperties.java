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

    @Data
    public static class Rag {
        /**
         * lexical：关键词，不调 embedding（默认，智谱未开通向量时用）
         * embedding：必须走向量模型
         * auto：有向量则向量，失败或全空则关键词
         */
        private String mode = "lexical";
        private boolean ingestOnStartup = true;
        private int topK = 5;
    }
}
