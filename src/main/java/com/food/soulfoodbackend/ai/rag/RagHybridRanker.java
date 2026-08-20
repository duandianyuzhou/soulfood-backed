package com.food.soulfoodbackend.ai.rag;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion：关键词榜 + 向量榜合并。distance 越小越靠前。
 */
public final class RagHybridRanker {

    static final int RRF_K = 60;

    private RagHybridRanker() {
    }

    public static List<RagHit> fuse(List<RagHit> lexical, List<RagHit> vector, int limit) {
        Map<String, Acc> merged = new LinkedHashMap<>();
        addRanks(merged, lexical, 1.0);
        addRanks(merged, vector, 1.0);
        int topK = Math.max(1, limit);
        List<Acc> ranked = new ArrayList<>(merged.values());
        ranked.sort(Comparator
                .comparingDouble((Acc a) -> a.score).reversed()
                .thenComparing(a -> a.hit.getSourceKey() == null ? "" : a.hit.getSourceKey()));
        List<RagHit> out = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, ranked.size()); i++) {
            Acc acc = ranked.get(i);
            RagHit hit = copy(acc.hit);
            hit.setDistance(1.0 / (1.0 + acc.score));
            out.add(hit);
        }
        return out;
    }

    private static void addRanks(Map<String, Acc> merged, List<RagHit> hits, double listWeight) {
        if (hits == null || hits.isEmpty()) {
            return;
        }
        for (int i = 0; i < hits.size(); i++) {
            RagHit hit = hits.get(i);
            String key = keyOf(hit);
            if (key == null) {
                continue;
            }
            double rrf = listWeight / (RRF_K + i + 1);
            Acc acc = merged.get(key);
            if (acc == null) {
                acc = new Acc(copy(hit), 0);
                merged.put(key, acc);
            }
            acc.score += rrf * sourceBoost(hit.getSourceType());
        }
    }

    static double sourceBoost(String sourceType) {
        if ("faq".equals(sourceType)) {
            return 1.12;
        }
        if ("recipe".equals(sourceType)) {
            return 1.06;
        }
        return 1.0;
    }

    static String keyOf(RagHit hit) {
        if (hit == null) {
            return null;
        }
        if (StringUtils.hasText(hit.getSourceKey())) {
            return hit.getSourceKey();
        }
        return hit.getId() == null ? null : "id:" + hit.getId();
    }

    private static RagHit copy(RagHit src) {
        RagHit hit = new RagHit();
        hit.setId(src.getId());
        hit.setSourceType(src.getSourceType());
        hit.setSourceId(src.getSourceId());
        hit.setSourceKey(src.getSourceKey());
        hit.setTitle(src.getTitle());
        hit.setContent(src.getContent());
        hit.setDistance(src.getDistance());
        return hit;
    }

    private static final class Acc {
        private final RagHit hit;
        private double score;

        private Acc(RagHit hit, double score) {
            this.hit = hit;
            this.score = score;
        }
    }
}
