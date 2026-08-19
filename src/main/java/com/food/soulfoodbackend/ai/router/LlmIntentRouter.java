package com.food.soulfoodbackend.ai.router;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class LlmIntentRouter {

    private static final String PROMPT = """
            把用户消息分类为唯一意图，只输出 JSON，不要其它文字：
            {"intent":"nearby_eat|cook_from_fridge|recipe_rag|vote_room|open_react","confidence":0.0}
            说明：
            nearby_eat=搜附近吃什么/探店
            cook_from_fridge=冰箱做菜、看图做菜、按食材做菜、菜单图识菜（有图或只报食材都可以）
            recipe_rag=某道菜的做法步骤、产品怎么用、饮食常识
            vote_room=组局投票
            open_react=需要查真实店、收藏或其它工具，或无法判断
            别名也视为 cook_from_fridge：cook_from_photo、cook_from_ingredients、看图做菜、按食材做菜、冰箱做菜
            用户消息：%s
            是否有图片：%s
            是否有定位：%s
            """;

    private final ChatClient statelessChatClient;
    private final ObjectMapper objectMapper;

    public LlmIntentRouter(
            @Qualifier("statelessChatClient") ChatClient statelessChatClient,
            ObjectMapper objectMapper) {
        this.statelessChatClient = statelessChatClient;
        this.objectMapper = objectMapper;
    }

    public RouteDecision classify(String message, boolean hasImage, boolean hasLocation) {
        try {
            String raw = statelessChatClient.prompt()
                    .options(OpenAiChatOptions.builder().temperature(0.1).build())
                    .user(PROMPT.formatted(
                            message == null ? "" : message,
                            hasImage ? "是" : "否",
                            hasLocation ? "是" : "否"))
                    .call()
                    .content();
            return parse(raw);
        } catch (Exception ex) {
            log.warn("LLM 意图分类失败: {}", ex.getMessage());
            return new RouteDecision(ChatIntent.OPEN_REACT, "llm_error", 0.0);
        }
    }

    RouteDecision parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new RouteDecision(ChatIntent.OPEN_REACT, "llm_empty", 0.0);
        }
        try {
            String json = extractJson(raw);
            JsonNode node = objectMapper.readTree(json);
            ChatIntent intent = parseIntent(node.path("intent").asText(""));
            double confidence = node.path("confidence").asDouble(0.0);
            if (confidence < 0.6) {
                return new RouteDecision(ChatIntent.OPEN_REACT, "llm_low_confidence", confidence);
            }
            return new RouteDecision(intent, "llm", confidence);
        } catch (Exception ex) {
            log.warn("解析意图 JSON 失败: {}", raw);
            return new RouteDecision(ChatIntent.OPEN_REACT, "llm_parse", 0.0);
        }
    }

    private static ChatIntent parseIntent(String raw) {
        return switch (raw == null ? "" : raw.trim().toLowerCase()) {
            case "nearby_eat" -> ChatIntent.NEARBY_EAT;
            case "cook_from_fridge", "cook_from_photo", "cook_from_ingredients",
                    "看图做菜", "按食材做菜", "冰箱做菜", "菜单图" -> ChatIntent.COOK_FROM_FRIDGE;
            case "recipe_rag" -> ChatIntent.RECIPE_RAG;
            case "vote_room" -> ChatIntent.VOTE_ROOM;
            default -> ChatIntent.OPEN_REACT;
        };
    }

    private static String extractJson(String raw) {
        String text = raw.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
