package com.food.soulfoodbackend.ai.workflow;

import com.food.soulfoodbackend.dto.preference.PreferenceResponse;
import com.food.soulfoodbackend.dto.restaurant.RestaurantDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearbyEatWorkflowTest {

    @Test
    void extractsCuisineKeyword() {
        assertEquals("火锅", NearbyEatWorkflow.extractKeyword("附近有什么火锅"));
        assertEquals(null, NearbyEatWorkflow.extractKeyword("附近吃什么"));
    }

    @Test
    void locateContinueMatchesNearbyOrAuth() {
        assertTrue(NearbyEatWorkflow.isLocateContinue("附近吃什么"));
        assertTrue(NearbyEatWorkflow.isLocateContinue("已开启定位，继续"));
        assertTrue(!NearbyEatWorkflow.isLocateContinue("红烧肉怎么做"));
    }

    @Test
    void filtersAllergensAndCoriander() {
        List<RestaurantDto> items = List.of(
                shop("香菜牛肉面", "面"),
                shop("花生酱拌面", "面"),
                shop("清汤锅", "火锅"));
        PreferenceResponse pref = new PreferenceResponse(
                List.of(),
                1,
                1,
                true,
                false,
                List.of("花生"),
                "不吃香菜、花生过敏");
        List<RestaurantDto> kept = NearbyEatWorkflow.filterByPreference(items, pref);
        assertEquals(1, kept.size());
        assertEquals("清汤锅", kept.get(0).getName());
    }

    @Test
    void keepsOriginalWhenFilterEmptiesNothingSpecial() {
        List<RestaurantDto> items = List.of(shop("川味小馆", "川菜"));
        assertTrue(NearbyEatWorkflow.filterByPreference(items, null).contains(items.get(0)));
    }

    private static RestaurantDto shop(String name, String category) {
        return new RestaurantDto(
                1L,
                name,
                category,
                BigDecimal.ONE,
                BigDecimal.ONE,
                "addr",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                false,
                false);
    }
}
