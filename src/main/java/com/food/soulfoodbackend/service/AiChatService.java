package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.dto.ai.RandomPickRequest;
import com.food.soulfoodbackend.dto.ai.RandomPickResponse;
import com.food.soulfoodbackend.dto.ai.RecipeItemDto;
import com.food.soulfoodbackend.dto.ai.RecommendRecipesRequest;
import com.food.soulfoodbackend.dto.ai.RecommendRecipesResponse;
import com.food.soulfoodbackend.dto.ai.SuggestOptionsRequest;
import com.food.soulfoodbackend.dto.ai.SuggestOptionsResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public AiChatService(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    public String resolveConversationId(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationId;
        }
        return UUID.randomUUID().toString();
    }

    public String chat(String conversationId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
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
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
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
        String raw = chatClient.prompt().user(prompt).call().content();
        List<RecipeItemDto> items = AiResponseParser.parseRecipes(raw);
        if (items.isEmpty()) {
            items = List.of(
                    new RecipeItemDto("番茄炒蛋", 4.8, "清淡下饭"),
                    new RecipeItemDto("土豆丝炒肉", 4.6, "家常菜"),
                    new RecipeItemDto("青菜鸡蛋面", 4.7, "快手暖胃")
            );
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
        String raw = chatClient.prompt().user(prompt).call().content();
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
        String raw = chatClient.prompt().user(prompt).call().content();
        AiResponseParser.RandomPickResult pick = AiResponseParser.parsePick(raw, candidates);
        return new RandomPickResponse(pick.name(), pick.reason(), pick.rawReply());
    }
}
