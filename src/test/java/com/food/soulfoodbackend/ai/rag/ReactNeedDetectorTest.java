package com.food.soulfoodbackend.ai.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactNeedDetectorTest {

    @Test
    void detectsToolHeavyIntents() {
        assertTrue(ReactNeedDetector.needsReact("附近有什么火锅"));
        assertTrue(ReactNeedDetector.needsReact("帮我开个投票组局"));
        assertFalse(ReactNeedDetector.needsReact("今天心情一般想吃点清淡的"));
        assertFalse(ReactNeedDetector.needsReact("红烧肉怎么做"));
    }
}
