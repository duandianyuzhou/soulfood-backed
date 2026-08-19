package com.food.soulfoodbackend.ai.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmIntentRouterTest {

    private final LlmIntentRouter router = new LlmIntentRouter(null, new ObjectMapper());

    @Test
    void parsesHighConfidenceIntent() {
        RouteDecision decision = router.parse("""
                {"intent":"nearby_eat","confidence":0.91}
                """);
        assertEquals(ChatIntent.NEARBY_EAT, decision.intent());
        assertEquals("llm", decision.source());
    }

    @Test
    void lowConfidenceFallsBackToReact() {
        RouteDecision decision = router.parse("""
                分类结果：{"intent":"nearby_eat","confidence":0.4}
                """);
        assertEquals(ChatIntent.OPEN_REACT, decision.intent());
        assertEquals("llm_low_confidence", decision.source());
    }

    @Test
    void invalidJsonFallsBackToReact() {
        RouteDecision decision = router.parse("not-json");
        assertEquals(ChatIntent.OPEN_REACT, decision.intent());
    }
}
