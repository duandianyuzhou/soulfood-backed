package com.food.soulfoodbackend.ai.react;

import com.food.soulfoodbackend.config.AiRagProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallGuardTest {

    @Test
    void capsCallsPerConversation() {
        ToolCallGuard guard = newGuard(5, 8000);
        guard.begin("c1");
        for (int i = 0; i < guard.maxCalls(); i++) {
            assertTrue(guard.tryAcquire("searchRecipes"));
        }
        assertFalse(guard.tryAcquire("searchRecipes"));
        guard.end();
        guard.begin("c2");
        assertTrue(guard.tryAcquire("searchNearbyRestaurants"));
        guard.end();
        guard.shutdown();
    }

    @Test
    void timeoutReturnsObservation() {
        ToolCallGuard guard = newGuard(5, 40);
        guard.begin("c1");
        String json = guard.execute("slowTool", () -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        });
        assertTrue(json.contains("超时"));
        guard.end();
        guard.shutdown();
    }

    @Test
    void failureReturnsObservation() {
        ToolCallGuard guard = newGuard(5, 8000);
        guard.begin("c1");
        String json = guard.execute("boom", () -> {
            throw new IllegalStateException("db down");
        });
        assertTrue(json.contains("失败"));
        assertTrue(json.contains("db down"));
        guard.end();
        guard.shutdown();
    }

    private static ToolCallGuard newGuard(int maxCalls, int timeoutMs) {
        AiRagProperties properties = new AiRagProperties();
        properties.setToolMaxCalls(maxCalls);
        properties.setToolTimeoutMs(timeoutMs);
        return new ToolCallGuard(properties);
    }
}
