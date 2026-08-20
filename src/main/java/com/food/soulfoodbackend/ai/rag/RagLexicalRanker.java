package com.food.soulfoodbackend.ai.rag;

import com.food.soulfoodbackend.domain.entity.SfRagChunk;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RagLexicalRanker {

    private RagLexicalRanker() {
    }

    public static List<RagHit> rank(String query, List<SfRagChunk> chunks, int limit) {
        if (!StringUtils.hasText(query) || chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        String q = query.trim();
        List<String> tokens = tokenize(q);
        return chunks.stream()
                .map(chunk -> toHit(chunk, score(q, tokens, chunk)))
                .filter(hit -> hit.getDistance() < 1.0)
                .sorted(Comparator.comparing(RagHit::getDistance))
                .limit(Math.max(1, limit))
                .toList();
    }

    static List<String> tokenize(String query) {
        Set<String> tokens = new LinkedHashSet<>();
        tokens.add(query);
        String compact = query.replaceAll("\\s+", "");
        if (compact.length() >= 2) {
            tokens.add(compact);
        }
        for (int n = 2; n <= 4; n++) {
            for (int i = 0; i + n <= compact.length(); i++) {
                String gram = compact.substring(i, i + n);
                if (gram.codePoints().anyMatch(RagLexicalRanker::isCjk)) {
                    tokens.add(gram);
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    private static RagHit toHit(SfRagChunk chunk, int score) {
        RagHit hit = new RagHit();
        hit.setId(chunk.getId());
        hit.setSourceType(chunk.getSourceType());
        hit.setSourceId(chunk.getSourceId());
        hit.setSourceKey(chunk.getSourceKey());
        hit.setTitle(chunk.getTitle());
        hit.setContent(chunk.getContent());
        double distance = score <= 0 ? 1.0 : 1.0 / (1.0 + score);
        hit.setDistance(distance);
        return hit;
    }

    static int score(String query, List<String> tokens, SfRagChunk chunk) {
        String title = chunk.getTitle() == null ? "" : chunk.getTitle();
        String content = chunk.getContent() == null ? "" : chunk.getContent();
        int s = 0;
        if (title.contains(query) || content.contains(query)) {
            s += 40;
        }
        for (String token : tokens) {
            if (token.length() < 2) {
                continue;
            }
            if (title.contains(token)) {
                s += 6 + token.length();
            }
            if (content.contains(token)) {
                s += 2 + Math.min(token.length(), 4);
            }
        }
        return s;
    }

    private static boolean isCjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA;
    }
}
