package com.food.soulfoodbackend.ai.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeQueryDetectorTest {

    @Test
    void detectsRecipeAndFaqQuestions() {
        assertTrue(KnowledgeQueryDetector.looksLikeKnowledgeQuery("红烧肉怎么做"));
        assertTrue(KnowledgeQueryDetector.looksLikeKnowledgeQuery("怎么加入房间"));
        assertTrue(KnowledgeQueryDetector.looksLikeKnowledgeQuery("隔夜米饭能吃吗"));
        assertFalse(KnowledgeQueryDetector.looksLikeKnowledgeQuery("附近有什么火锅"));
        assertFalse(KnowledgeQueryDetector.looksLikeKnowledgeQuery(""));
    }
}
