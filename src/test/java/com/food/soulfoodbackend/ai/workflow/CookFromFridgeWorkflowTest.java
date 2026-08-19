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

    @Test
    void skipPhotoPhrases() {
        assertEquals(true, CookFromFridgeWorkflow.isSkipPhoto("不拍图"));
        assertEquals(true, CookFromFridgeWorkflow.isSkipPhoto("懒得拍，用文字"));
        assertEquals(false, CookFromFridgeWorkflow.isSkipPhoto("番茄鸡蛋怎么做"));
    }

    @Test
    void extractsTextIngredients() {
        assertEquals(List.of("番茄", "鸡蛋"), CookFromFridgeWorkflow.extractTextIngredients("冰箱有番茄和鸡蛋"));
        assertEquals(List.of(), CookFromFridgeWorkflow.extractTextIngredients("不拍图"));
    }

    @Test
    void titlesCoverFridgePhotoAndIngredients() {
        assertEquals("冰箱做菜", CookFromFridgeWorkflow.titleOf("冰箱里有番茄", false));
        assertEquals("看图做菜", CookFromFridgeWorkflow.titleOf("看图做菜", false));
        assertEquals("看图做菜", CookFromFridgeWorkflow.titleOf("随便", true));
        assertEquals("按食材做菜", CookFromFridgeWorkflow.titleOf("按食材做菜", false));
    }
}
