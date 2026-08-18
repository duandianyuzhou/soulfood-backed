package com.food.soulfoodbackend.ai.rag;

import com.food.soulfoodbackend.config.AiRagProperties;
import com.food.soulfoodbackend.mapper.SfRagChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
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
        String vector = embeddingClient.toPgVector(embeddingClient.embed(query.trim()));
        return chunkMapper.searchNearest(vector, topK);
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

    private static String label(String sourceType) {
        return switch (sourceType) {
            case "recipe" -> "菜谱";
            case "faq" -> "产品FAQ";
            case "common" -> "饮食常识";
            default -> sourceType == null ? "未知" : sourceType;
        };
    }
}
