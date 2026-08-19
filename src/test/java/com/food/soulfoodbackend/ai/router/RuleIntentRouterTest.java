package com.food.soulfoodbackend.ai.router;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RuleIntentRouterTest {

    @Test
    void routesImageToFridge() {
        assertEquals(ChatIntent.COOK_FROM_FRIDGE, RuleIntentRouter.tryRoute("随便", true, false));
    }

    @Test
    void routesKnowledgeToRag() {
        assertEquals(ChatIntent.RECIPE_RAG, RuleIntentRouter.tryRoute("红烧肉怎么做", false, false));
        assertEquals(ChatIntent.RECIPE_RAG, RuleIntentRouter.tryRoute("怎么加入房间", false, false));
    }

    @Test
    void routesNearbySearchToEat() {
        assertEquals(ChatIntent.NEARBY_EAT, RuleIntentRouter.tryRoute("附近有什么火锅", false, false));
        assertEquals(ChatIntent.NEARBY_EAT, RuleIntentRouter.tryRoute("今晚吃什么", false, true));
    }

    @Test
    void tonightWithoutLocationIsAmbiguous() {
        assertNull(RuleIntentRouter.tryRoute("今晚吃什么", false, false));
        assertNull(RuleIntentRouter.tryRoute("随便吃", false, false));
    }

    @Test
    void routesVoteAndChat() {
        assertEquals(ChatIntent.VOTE_ROOM, RuleIntentRouter.tryRoute("帮我开个投票组局", false, false));
        assertEquals(ChatIntent.SIMPLE_CHAT, RuleIntentRouter.tryRoute("今天心情一般想吃点清淡的", false, false));
    }
}
