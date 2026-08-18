package com.food.soulfoodbackend.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.config.AiRagProperties;
import com.food.soulfoodbackend.domain.entity.SfRagChunk;
import com.food.soulfoodbackend.mapper.SfRagChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagSearchService {

    private final EmbeddingClient embeddingClient;
    private final SfRagChunkMapper chunkMapper;
    private final AiRagProperties properties;

    public List<RagHit> search(String query) {
        return search(query, properties.getRag().getTopK());
    }

    public List<RagHit> search(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        int topK = Math.max(1, Math.min(limit, 10));
        String mode = mode();
        if ("lexical".equals(mode)) {
            return searchLexical(query, topK);
        }
        if ("embedding".equals(mode)) {
            return searchVector(query, topK);
        }
        try {
            List<RagHit> hits = searchVector(query, topK);
            if (!hits.isEmpty()) {
                return hits;
            }
        } catch (Exception ex) {
            log.warn("向量检索失败，回退关键词: {}", ex.getMessage());
        }
        return searchLexical(query, topK);
    }

    private List<RagHit> searchVector(String query, int topK) {
        String vector = embeddingClient.toPgVector(embeddingClient.embed(query.trim()));
        return chunkMapper.searchNearest(vector, topK);
    }

    private List<RagHit> searchLexical(String query, int topK) {
        List<SfRagChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<SfRagChunk>()
                .orderByAsc(SfRagChunk::getId));
        return RagLexicalRanker.rank(query.trim(), chunks, topK);
    }

    public String formatForPrompt(List<RagHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "（无检索结果）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            RagHit hit = hits.get(i);
            sb.append(i + 1)
                    .append(". [")
                    .append(label(hit.getSourceType()))
                    .append("] ")
                    .append(hit.getTitle() == null ? "" : hit.getTitle())
                    .append('\n')
                    .append(hit.getContent())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }

    private String mode() {
        String mode = properties.getRag().getMode();
        return mode == null ? "lexical" : mode.trim().toLowerCase(Locale.ROOT);
    }

    private static String label(String sourceType) {
        return switch (sourceType) {
            case "recipe" -> "菜谱";
            case "faq" -> "产品FAQ";
            case "common" -> "饮食常识";
            default -> sourceType == null ? "未知" : sourceType;
        };
    }
}
