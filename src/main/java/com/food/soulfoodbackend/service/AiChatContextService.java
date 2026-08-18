package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.dto.favorite.FavoriteItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiChatContextService {

    private static final String SIMPLE_INSTRUCTION = """
            你是 DecideMeal 美食助手，擅长中餐推荐、菜谱讲解和饮食搭配。
            回答要简洁实用，语气亲切。不要编造不存在的店名、房间号或本 App 功能。
            如果用户要搜附近餐厅、创建投票或收藏，请提醒对方说明「附近」「组局」「收藏」等具体需求。
            """;

    private static final String BASE_INSTRUCTION = """
            你是 DecideMeal 美食助手，擅长中餐推荐、菜谱讲解、探店建议和饮食搭配。
            回答要简洁实用，语气亲切，优先给出可操作的推荐。
            推荐菜品或餐厅时，尽量使用真实常见的名称，方便用户后续查看详情。
            你可以调用工具查询真实数据并执行操作，不要编造店名或菜名：
            - searchNearbyRestaurants：查附近餐厅（需定位）
            - searchRecipes：查菜品库
            - searchKnowledge：检索 FAQ、饮食常识和菜谱知识
            - createVoteRoom：创建组局投票（至少 2 个选项）
            - addFavorite：收藏菜谱或餐厅
            当用户需要真实推荐、发起投票或收藏时，优先调用工具。
            用户问怎么做菜、产品怎么用、隔夜菜等知识时，优先调用 searchKnowledge。
            """;

    private final UserPreferenceService preferenceService;
    private final FavoriteService favoriteService;
    private final AiUserMemoryService memoryService;

    public String buildSystemPrompt(Long userId, Double lat, Double lng) {
        return buildPrompt(BASE_INSTRUCTION, userId, lat, lng);
    }

    public String buildSimpleSystemPrompt(Long userId, Double lat, Double lng) {
        return buildPrompt(SIMPLE_INSTRUCTION, userId, lat, lng);
    }

    private String buildPrompt(String instruction, Long userId, Double lat, Double lng) {
        StringBuilder sb = new StringBuilder(instruction);
        sb.append("\n\n【用户上下文】");
        if (userId != null) {
            sb.append("\n- 口味偏好：").append(preferenceService.buildPreferenceText(userId));
            var favorites = favoriteService.listFavorites(userId, null).stream().limit(8).toList();
            if (!favorites.isEmpty()) {
                sb.append("\n- 最近收藏：")
                        .append(favorites.stream().map(FavoriteItemDto::getTitle)
                                .reduce((a, b) -> a + "、" + b).orElse(""));
            }
            sb.append(memoryService.buildMemoryContextBlock(userId));
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

    public String buildVisionSystemPrompt(Long userId, Double lat, Double lng) {
        return buildSystemPrompt(userId, lat, lng) + """
                
                【识图模式】用户发送了图片，可能是冰箱食材、菜单或菜品照片。
                请识别可见食材/菜品，推荐可做的菜或点单建议，语气简洁实用。
                """;
    }
}
