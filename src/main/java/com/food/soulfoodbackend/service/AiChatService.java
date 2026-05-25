package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.dto.ai.RandomPickRequest;
import com.food.soulfoodbackend.dto.ai.RandomPickResponse;
import com.food.soulfoodbackend.dto.ai.RecipeItemDto;
import com.food.soulfoodbackend.dto.ai.RecommendRecipesRequest;
import com.food.soulfoodbackend.dto.ai.RecommendRecipesResponse;
import com.food.soulfoodbackend.dto.ai.SuggestOptionsRequest;
import com.food.soulfoodbackend.dto.ai.SuggestOptionsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AiChatService {

    private final ChatClient chatClient;
    private final ChatClient statelessChatClient;
    private final ChatMemory chatMemory;

    public AiChatService(
            @Qualifier("chatClient") ChatClient chatClient,
            @Qualifier("statelessChatClient") ChatClient statelessChatClient,
            ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.statelessChatClient = statelessChatClient;
        this.chatMemory = chatMemory;
    }

    public String resolveConversationId(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationId;
        }
        return UUID.randomUUID().toString();
    }

    public String chat(String conversationId, String message) {
        return safeCall(() -> chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content(), "今天可以试试番茄炒蛋，简单又下饭。");
    }

    public Flux<String> chatStream(String conversationId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    public List<Message> getHistory(String conversationId) {
        return chatMemory.get(conversationId);
    }

    public void clearMemory(String conversationId) {
        chatMemory.clear(conversationId);
    }

    public String recommend(String preference) {
        String prompt = """
                请根据以下饮食偏好推荐 3 道适合的家常菜，并简要说明理由：
                %s
                """.formatted(preference);
        return safeCall(() -> statelessChatClient.prompt()
                .user(prompt)
                .call()
                .content(), fallbackRecipeText(preference, null));
    }

    public RecommendRecipesResponse recommendRecipes(RecommendRecipesRequest request) {
        String preference = request.getPreference() != null ? request.getPreference() : "清淡、家常菜";
        String winner = request.getVoteWinner() != null ? request.getVoteWinner() : "";
        String prompt = """
                根据饮食偏好「%s」%s，推荐 3 道可在家做的家常菜。
                严格只输出 JSON，不要其他文字：
                {"items":[{"name":"菜名","score":4.8,"reason":"一句理由"}]}
                """.formatted(
                preference,
                winner.isBlank() ? "" : "，投票胜出选项是「" + winner + "」");

        String raw;
        boolean aiFailed;
        try {
            raw = statelessChatClient.prompt().user(prompt).call().content();
            aiFailed = raw == null || raw.isBlank();
        } catch (Exception ex) {
            log.warn("AI recommendRecipes failed: {}", ex.getMessage());
            aiFailed = true;
            raw = "";
        }

        List<RecipeItemDto> items;
        if (aiFailed) {
            items = defaultRecipeItems(winner);
            raw = formatRecipeReply(items);
        } else {
            items = AiResponseParser.parseRecipes(raw);
            if (items.isEmpty()) {
                items = defaultRecipeItems(winner);
                raw = formatRecipeReply(items);
            }
        }
        return new RecommendRecipesResponse(request.getConversationId(), items, raw);
    }

    public SuggestOptionsResponse suggestOptions(SuggestOptionsRequest request) {
        String topic = request.getTopic() != null ? request.getTopic() : "今晚吃什么";
        String existing = request.getExistingOptions() != null
                ? String.join("、", request.getExistingOptions())
                : "无";
        String prompt = """
                为聚餐主题「%s」再生成 3 个投票选项（每个 2-8 个字）。
                已有选项：%s
                每行只写一个选项名称，不要编号和解释。
                """.formatted(topic, existing);
        String raw = safeCall(() -> statelessChatClient.prompt().user(prompt).call().content(), "寿司\n麻辣烫\n黄焖鸡");
        List<String> options = AiResponseParser.parseOptions(raw);
        if (options.isEmpty()) {
            options = List.of("寿司", "麻辣烫", "黄焖鸡");
        }
        return new SuggestOptionsResponse(options, raw);
    }

    public RandomPickResponse randomPick(RandomPickRequest request) {
        List<String> candidates = request.getCandidates() != null ? request.getCandidates() : List.of();
        if (candidates.isEmpty()) {
            return new RandomPickResponse("暂无推荐", "候选列表为空", "");
        }
        String pref = request.getPreference() != null ? request.getPreference() : "";
        String prompt = """
                从以下餐厅中选一家今天去吃，结合偏好「%s」。
                候选：%s
                严格只输出 JSON：{"name":"店名","reason":"一句话理由"}
                """.formatted(pref, String.join("、", candidates));
        String raw = safeCall(() -> statelessChatClient.prompt().user(prompt).call().content(),
                "{\"name\":\"" + candidates.get(0) + "\",\"reason\":\"距离近、评分不错\"}");
        AiResponseParser.RandomPickResult pick = AiResponseParser.parsePick(raw, candidates);
        return new RandomPickResponse(pick.name(), pick.reason(), pick.rawReply());
    }

    private String safeCall(java.util.function.Supplier<String> supplier, String fallback) {
        try {
            String result = supplier.get();
            if (result == null || result.isBlank()) {
                return fallback;
            }
            return result;
        } catch (Exception ex) {
            log.warn("AI call failed, using fallback: {}", ex.getMessage());
            return fallback;
        }
    }

    private List<RecipeItemDto> defaultRecipeItems(String winner) {
        if (winner != null && winner.contains("火锅")) {
            return List.of(
                    new RecipeItemDto("毛肚", 4.8, "火锅经典，口感爽脆"),
                    new RecipeItemDto("肥牛卷", 4.7, "搭配火锅很合适"),
                    new RecipeItemDto("娃娃菜", 4.6, "解腻必备"));
        }
        return List.of(
                new RecipeItemDto("番茄炒蛋", 4.8, "清淡下饭"),
                new RecipeItemDto("土豆丝炒肉", 4.6, "家常菜"),
                new RecipeItemDto("青菜鸡蛋面", 4.7, "快手暖胃"));
    }

    private String formatRecipeReply(List<RecipeItemDto> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            RecipeItemDto item = items.get(i);
            sb.append(i + 1).append(". ").append(item.getName()).append("：").append(item.getReason());
            if (i < items.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String fallbackRecipeText(String preference, String winner) {
        return String.join("\n",
                "1. 番茄炒蛋：清淡下饭",
                "2. 土豆丝炒肉：家常菜",
                "3. 青菜鸡蛋面：快手暖胃")
                + (winner != null && !winner.isBlank() ? "（搭配「" + winner + "」）" : "");
    }
}
