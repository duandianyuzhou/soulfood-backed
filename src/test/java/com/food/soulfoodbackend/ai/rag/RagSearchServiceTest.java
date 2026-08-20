package com.food.soulfoodbackend.ai.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagSearchServiceTest {

    @Test
    void escapesLikeWildcards() {
        assertEquals("%hello%", RagSearchService.likePattern("hello"));
        assertEquals("%100!%%", RagSearchService.likePattern("100%"));
        assertEquals("%a!_b%", RagSearchService.likePattern("a_b"));
        assertEquals("%!!x%", RagSearchService.likePattern("!x"));
    }
}
