package com.food.soulfoodbackend.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapacityExceededTest {

    @Test
    void detectsZhipu1305() {
        Throwable error = new NonTransientAiException(
                "HTTP 429 - {\"error\":{\"code\":\"1305\",\"message\":\"该模型当前访问量过大，请您稍后再试\"}}");
        assertTrue(CapacityExceeded.of(error));
    }

    @Test
    void detectsWrapped429() {
        Throwable error = new RuntimeException("call failed", new RuntimeException("HTTP 429 Too Many Requests"));
        assertTrue(CapacityExceeded.of(error));
    }

    @Test
    void ignoresNormalErrors() {
        assertFalse(CapacityExceeded.of(new RuntimeException("connection refused")));
    }
}
