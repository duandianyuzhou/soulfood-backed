package com.food.soulfoodbackend.ai.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagHybridRankerTest {

    @Test
    void bothListsBoostOverlap() {
        List<RagHit> lexical = List.of(hit("faq.a", "faq"), hit("common.b", "common"), hit("faq.c", "faq"));
        List<RagHit> vector = List.of(hit("common.b", "common"), hit("faq.a", "faq"), hit("recipe.d", "recipe"));
        List<RagHit> fused = RagHybridRanker.fuse(lexical, vector, 3);
        assertEquals("faq.a", fused.get(0).getSourceKey());
        assertEquals("common.b", fused.get(1).getSourceKey());
    }

    @Test
    void emptyVectorKeepsLexicalOrderViaCallerContract() {
        List<RagHit> lexical = List.of(hit("faq.room.join", "faq"), hit("common.leftover.rice", "common"));
        List<RagHit> fused = RagHybridRanker.fuse(lexical, List.of(), 2);
        assertEquals("faq.room.join", fused.get(0).getSourceKey());
        assertEquals(2, fused.size());
    }

    private static RagHit hit(String key, String type) {
        RagHit row = new RagHit();
        row.setSourceKey(key);
        row.setSourceType(type);
        row.setTitle(key);
        row.setContent(key);
        row.setDistance(0.5);
        return row;
    }
}
