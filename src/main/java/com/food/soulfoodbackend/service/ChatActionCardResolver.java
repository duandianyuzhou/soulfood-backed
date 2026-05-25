package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.domain.entity.SfRecipe;
import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import com.food.soulfoodbackend.dto.restaurant.RestaurantDto;
import com.food.soulfoodbackend.mapper.SfRecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatActionCardResolver {

    private static final int MAX_CARDS = 4;

    private final SfRecipeMapper recipeMapper;
    private final RestaurantService restaurantService;

    public List<ChatActionCardDto> resolve(String reply, Long userId, Double lat, Double lng) {
        return merge(AiChatToolContextHolder.toolCards(), resolveFromText(reply, userId, lat, lng));
    }

    private List<ChatActionCardDto> resolveFromText(String reply, Long userId, Double lat, Double lng) {
        if (reply == null || reply.isBlank()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<ChatActionCardDto> cards = new ArrayList<>();

        for (SfRecipe recipe : recipeMapper.selectList(null)) {
            if (recipe.getName() == null || !reply.contains(recipe.getName())) {
                continue;
            }
            String key = "recipe:" + recipe.getName();
            if (!seen.add(key)) {
                continue;
            }
            cards.add(new ChatActionCardDto(
                    "recipe",
                    recipe.getId(),
                    recipe.getName(),
                    recipe.getCategory()));
            if (cards.size() >= MAX_CARDS) {
                return cards;
            }
        }

        if (userId != null && lat != null && lng != null) {
            try {
                List<RestaurantDto> nearby = restaurantService.listNearby(userId, lng, lat, null, null);
                for (RestaurantDto restaurant : nearby) {
                    if (restaurant.getName() == null || !reply.contains(restaurant.getName())) {
                        continue;
                    }
                    String key = "restaurant:" + restaurant.getName();
                    if (!seen.add(key)) {
                        continue;
                    }
                    cards.add(new ChatActionCardDto(
                            "restaurant",
                            restaurant.getId(),
                            restaurant.getName(),
                            restaurant.getCategory()));
                    if (cards.size() >= MAX_CARDS) {
                        break;
                    }
                }
            } catch (Exception ignored) {
                // 卡片解析失败不影响主回复
            }
        }
        return cards;
    }

    private List<ChatActionCardDto> merge(List<ChatActionCardDto> toolCards, List<ChatActionCardDto> textCards) {
        Set<String> seen = new LinkedHashSet<>();
        List<ChatActionCardDto> merged = new ArrayList<>();
        for (ChatActionCardDto card : toolCards) {
            String key = card.getType() + ":" + card.getId() + ":" + card.getName();
            if (seen.add(key)) {
                merged.add(card);
            }
        }
        for (ChatActionCardDto card : textCards) {
            String key = card.getType() + ":" + card.getId() + ":" + card.getName();
            if (seen.add(key)) {
                merged.add(card);
            }
            if (merged.size() >= MAX_CARDS) {
                break;
            }
        }
        return merged.size() > MAX_CARDS ? merged.subList(0, MAX_CARDS) : merged;
    }
}
