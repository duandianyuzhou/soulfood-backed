package com.food.soulfoodbackend.ai.react;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallGuardTest {

    @Test
    void capsCallsPerConversation() {
        ToolCallGuard guard = new ToolCallGuard();
        guard.begin("c1");
        for (int i = 0; i < ToolCallGuard.MAX_TOOL_CALLS; i++) {
            assertTrue(guard.tryAcquire("searchRecipes"));
        }
        assertFalse(guard.tryAcquire("searchRecipes"));
        guard.end();
        guard.begin("c2");
        assertTrue(guard.tryAcquire("searchNearbyRestaurants"));
        guard.end();
    }
}
