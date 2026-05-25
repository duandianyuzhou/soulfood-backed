package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.dto.favorite.FavoriteItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiChatContextService {

    private static final String BASE_INSTRUCTION = """
            你是 DecideMeal 美食助手，擅长中餐推荐、菜谱讲解、探店建议和饮食搭配。
            回答要简洁实用，语气亲切，优先给出可操作的推荐。
            推荐菜品或餐厅时，尽量使用真实常见的名称，方便用户后续查看详情。
            """;

    private final UserPreferenceService preferenceService;
    private final FavoriteService favoriteService;

    public String buildSystemPrompt(Long userId, Double lat, Double lng) {
        StringBuilder sb = new StringBuilder(BASE_INSTRUCTION);
        sb.append("\n\n【用户上下文】");
        if (userId != null) {
            sb.append("\n- 口味偏好：").append(preferenceService.buildPreferenceText(userId));
            var favorites = favoriteService.listFavorites(userId, null).stream().limit(8).toList();
            if (!favorites.isEmpty()) {
                sb.append("\n- 最近收藏：")
                        .append(favorites.stream().map(FavoriteItemDto::getTitle)
                                .reduce((a, b) -> a + "、" + b).orElse(""));
            }
        } else {
            sb.append("\n- 口味偏好：未登录，按大众口味推荐");
        }
        if (lat != null && lng != null) {
            sb.append("\n- 当前位置：纬度 ").append(lat).append("，经度 ").append(lng)
                    .append("（回答探店、附近吃什么时请考虑距离）");
        }
        sb.append("\n请结合以上信息个性化回答，避免推荐用户忌口的内容。");
        return sb.toString();
    }
}
