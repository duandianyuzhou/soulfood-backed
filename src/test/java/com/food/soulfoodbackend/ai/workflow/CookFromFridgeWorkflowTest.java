package com.food.soulfoodbackend.ai.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CookFromFridgeWorkflowTest {

    @Test
    void parsesIngredientJson() {
        assertEquals(
                List.of("番茄", "鸡蛋"),
                CookFromFridgeWorkflow.parseIngredients("""
                        好的
                        {"ingredients":["番茄","鸡蛋"]}
                        """));
        assertEquals(List.of(), CookFromFridgeWorkflow.parseIngredients("没有json"));
    }
}
