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
    void parsesCookAliases() {
        assertEquals(ChatIntent.COOK_FROM_FRIDGE, router.parse("""
                {"intent":"看图做菜","confidence":0.9}
                """).intent());
        assertEquals(ChatIntent.COOK_FROM_FRIDGE, router.parse("""
                {"intent":"cook_from_ingredients","confidence":0.88}
                """).intent());
        assertEquals(ChatIntent.COOK_FROM_FRIDGE, router.parse("""
                {"intent":"按食材做菜","confidence":0.86}
                """).intent());
    }

    @Test
    void invalidJsonFallsBackToReact() {
        RouteDecision decision = router.parse("not-json");
        assertEquals(ChatIntent.OPEN_REACT, decision.intent());
    }
}
