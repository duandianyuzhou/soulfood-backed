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
        assertTrue(ReactNeedDetector.needsNearbySearch("附近有什么寿司"));
        assertFalse(ReactNeedDetector.needsNearbySearch("今晚想吃清淡点"));
    }
}
