package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.ai.rag.RagHit;
import com.food.soulfoodbackend.ai.rag.RagIngestService;
import com.food.soulfoodbackend.ai.rag.RagSearchService;
import com.food.soulfoodbackend.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 开发调试：知识库入库与检索。App 主路径仍走 /ai/chat。
 */
@RestController
@RequestMapping("/ai/rag")
public class RagController {

    private final RagIngestService ingestService;
    private final RagSearchService searchService;

    public RagController(RagIngestService ingestService, RagSearchService searchService) {
        this.ingestService = ingestService;
        this.searchService = searchService;
    }

    @PostMapping("/ingest")
    public ApiResult<Map<String, Integer>> ingest() {
        RagIngestService.IngestReport report = ingestService.ingestAll();
        return ApiResult.ok(Map.of(
                "seedsUpserted", report.seedsUpserted(),
                "recipesUpserted", report.recipesUpserted()));
    }

    @GetMapping("/search")
    public ApiResult<List<RagHit>> search(
            @RequestParam String q,
            @RequestParam(required = false) Integer limit) {
        List<RagHit> hits = limit == null ? searchService.search(q) : searchService.search(q, limit);
        return ApiResult.ok(hits);
    }
}
