package com.food.soulfoodbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import com.food.soulfoodbackend.dto.favorite.AddFavoriteRequest;
import com.food.soulfoodbackend.dto.recipe.RecipeSummaryDto;
import com.food.soulfoodbackend.dto.restaurant.RestaurantDto;
import com.food.soulfoodbackend.dto.room.CreateRoomRequest;
import com.food.soulfoodbackend.dto.room.CreateRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiChatTools {

    private static final int MAX_RESULTS = 5;

    private final RestaurantService restaurantService;
    private final RecipeService recipeService;
    private final RoomService roomService;
    private final FavoriteService favoriteService;
    private final ObjectMapper objectMapper;

    @Tool(description = "搜索用户附近的餐厅，返回真实店名、距离、评分。用户问附近吃什么、探店、找餐厅时优先调用。")
    public String searchNearbyRestaurants(
            @ToolParam(description = "搜索关键词，如火锅、日料，可为空") String keyword,
            @ToolParam(description = "品类筛选，如川菜、火锅，可为空") String category) {
        Long userId = AiChatToolContextHolder.userId();
        Double lat = AiChatToolContextHolder.lat();
        Double lng = AiChatToolContextHolder.lng();
        if (userId == null || lat == null || lng == null) {
            return jsonError("需要用户登录并授权定位后才能搜索附近餐厅");
        }
        try {
            List<RestaurantDto> items = restaurantService.listNearby(
                    userId, lng, lat, blankToNull(category), blankToNull(keyword));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (RestaurantDto item : items.stream().limit(MAX_RESULTS).toList()) {
                rows.add(Map.of(
                        "id", item.getId(),
                        "name", item.getName(),
                        "category", nullSafe(item.getCategory()),
                        "rating", nullSafe(item.getRating()),
                        "distance", nullSafe(item.getDistanceKm())));
                AiChatToolContextHolder.addCard(new ChatActionCardDto(
                        "restaurant", item.getId(), item.getName(), item.getCategory()));
            }
            return json(Map.of("count", rows.size(), "items", rows));
        } catch (Exception ex) {
            log.warn("searchNearbyRestaurants failed: {}", ex.getMessage());
            return jsonError("搜索附近餐厅失败：" + ex.getMessage());
        }
    }

    @Tool(description = "搜索菜品库中的菜谱，返回真实菜名、分类、评分。用户问怎么做、推荐家常菜时优先调用。")
    public String searchRecipes(
            @ToolParam(description = "菜名或食材关键词，可为空") String keyword,
            @ToolParam(description = "分类，如家常菜、川菜，可为空") String category) {
        try {
            List<RecipeSummaryDto> items = recipeService.list(blankToNull(category), blankToNull(keyword));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (RecipeSummaryDto item : items.stream().limit(MAX_RESULTS).toList()) {
                rows.add(Map.of(
                        "id", item.getId(),
                        "name", item.getName(),
                        "category", nullSafe(item.getCategory()),
                        "score", nullSafe(item.getScore())));
                AiChatToolContextHolder.addCard(new ChatActionCardDto(
                        "recipe", item.getId(), item.getName(), item.getCategory()));
            }
            return json(Map.of("count", rows.size(), "items", rows));
        } catch (Exception ex) {
            log.warn("searchRecipes failed: {}", ex.getMessage());
            return jsonError("搜索菜谱失败：" + ex.getMessage());
        }
    }

    @Tool(description = "创建组局投票房间，用于和朋友一起决定吃什么。需要主题和至少 2 个投票选项。")
    public String createVoteRoom(
            @ToolParam(description = "投票主题，如「今晚吃什么」") String topic,
            @ToolParam(description = "投票选项列表，如 [\"火锅\",\"烤肉\",\"寿司\"]") List<String> options,
            @ToolParam(description = "最大人数，默认 4", required = false) Integer maxPeople) {
        Long userId = AiChatToolContextHolder.userId();
        if (userId == null) {
            return jsonError("需要登录后才能创建投票房间");
        }
        if (!StringUtils.hasText(topic)) {
            return jsonError("投票主题不能为空");
        }
        List<String> cleaned = options != null
                ? options.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList()
                : List.of();
        if (cleaned.size() < 2) {
            return jsonError("至少需要 2 个投票选项");
        }
        try {
            CreateRoomRequest request = new CreateRoomRequest();
            request.setTopic(topic.trim());
            request.setMaxPeople(maxPeople != null ? maxPeople : 4);
            request.setDurationMin(30);
            request.setInitialOptions(cleaned);
            CreateRoomResponse room = roomService.createRoom(userId, request);
            AiChatToolContextHolder.addCard(new ChatActionCardDto(
                    "vote_room", 0L, topic.trim(), room.getCode()));
            return json(Map.of(
                    "roomCode", room.getCode(),
                    "topic", room.getTopic(),
                    "options", cleaned));
        } catch (Exception ex) {
            log.warn("createVoteRoom failed: {}", ex.getMessage());
            return jsonError("创建投票房间失败：" + ex.getMessage());
        }
    }

    @Tool(description = "收藏菜谱或餐厅到用户收藏夹。type 为 recipe 或 restaurant，targetId 为对应 ID。")
    public String addFavorite(
            @ToolParam(description = "收藏类型：recipe 或 restaurant") String type,
            @ToolParam(description = "菜谱或餐厅的 ID") Long targetId,
            @ToolParam(description = "显示标题，通常用菜名或店名") String title) {
        Long userId = AiChatToolContextHolder.userId();
        if (userId == null) {
            return jsonError("需要登录后才能收藏");
        }
        if (!"recipe".equals(type) && !"restaurant".equals(type)) {
            return jsonError("收藏类型只能是 recipe 或 restaurant");
        }
        if (targetId == null || !StringUtils.hasText(title)) {
            return jsonError("targetId 和 title 不能为空");
        }
        try {
            AddFavoriteRequest request = new AddFavoriteRequest();
            request.setTargetType(type);
            request.setTargetId(targetId);
            request.setTitle(title.trim());
            favoriteService.addFavorite(userId, request);
            return json(Map.of("status", "ok", "type", type, "targetId", targetId, "title", title.trim()));
        } catch (Exception ex) {
            log.warn("addFavorite failed: {}", ex.getMessage());
            return jsonError("收藏失败：" + ex.getMessage());
        }
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String nullSafe(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private String json(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"序列化失败\"}";
        }
    }

    private String jsonError(String message) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("error", message);
        return json(payload);
    }
}
