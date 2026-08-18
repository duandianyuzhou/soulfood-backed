package com.food.soulfoodbackend.ai.rag;

import com.food.soulfoodbackend.config.AiRagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagIngestRunner implements ApplicationRunner {

    private final RagIngestService ingestService;
    private final AiRagProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getRag().isIngestOnStartup()) {
            log.info("RAG ingest-on-startup disabled");
            return;
        }
        try {
            ingestService.ingestAll();
        } catch (Exception ex) {
            log.warn("RAG 启动入库失败（对话仍可用，检索可能为空）: {}", ex.getMessage());
        }
    }
}
