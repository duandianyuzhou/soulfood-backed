package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.ChatRequest;
import com.food.soulfoodbackend.dto.ChatResponse;
import com.food.soulfoodbackend.dto.ai.AiConversationItemDto;
import com.food.soulfoodbackend.dto.ai.ChatHistoryMessageDto;
import com.food.soulfoodbackend.dto.ai.RandomPickRequest;
import com.food.soulfoodbackend.dto.ai.RandomPickResponse;
import com.food.soulfoodbackend.dto.ai.RecommendRecipesRequest;
import com.food.soulfoodbackend.dto.ai.RecommendRecipesResponse;
import com.food.soulfoodbackend.dto.ai.SuggestOptionsRequest;
import com.food.soulfoodbackend.dto.ai.SuggestOptionsResponse;
import com.food.soulfoodbackend.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ApiResult<ChatResponse> chat(@RequestBody ChatRequest request) {
        String conversationId = aiChatService.resolveConversationId(request.conversationId());
        ChatResponse response = aiChatService.chat(
                conversationId,
                request.message(),
                UserContext.getUserId(),
                request.lat(),
                request.lng());
        return ApiResult.ok(response);
    }

    @PostMapping(value = "/chat/stream", produces = "application/x-ndjson;charset=UTF-8")
    public Flux<String> chatStreamPost(@RequestBody ChatRequest request) {
        String resolvedId = aiChatService.resolveConversationId(request.conversationId());
        return aiChatService.chatStreamNdjson(
                resolvedId,
                request.message(),
                UserContext.getUserId(),
                request.lat(),
                request.lng());
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        String resolvedId = aiChatService.resolveConversationId(conversationId);
        return aiChatService.chatStream(resolvedId, message, UserContext.getUserId());
    }

    @GetMapping("/chat/{conversationId}/history")
    public ApiResult<List<ChatHistoryMessageDto>> history(@PathVariable String conversationId) {
        return ApiResult.ok(aiChatService.getHistoryMessages(conversationId, UserContext.getUserId()));
    }

    @GetMapping("/conversations")
    public ApiResult<List<AiConversationItemDto>> conversations() {
        return ApiResult.ok(aiChatService.listConversations(UserContext.getUserId()));
    }

    @DeleteMapping("/chat/{conversationId}")
    public ApiResult<Map<String, String>> clearMemory(@PathVariable String conversationId) {
        aiChatService.clearMemory(conversationId, UserContext.getUserId());
        return ApiResult.ok(Map.of("conversationId", conversationId, "status", "cleared"));
    }

    @GetMapping("/recommend")
    public ApiResult<ChatResponse> recommend(@RequestParam(defaultValue = "清淡、少油、适合晚餐") String preference) {
        return ApiResult.ok(new ChatResponse(null, aiChatService.recommend(preference), List.of()));
    }

    @PostMapping("/recommend/recipes")
    public ApiResult<RecommendRecipesResponse> recommendRecipes(@RequestBody RecommendRecipesRequest request) {
        return ApiResult.ok(aiChatService.recommendRecipes(request));
    }

    @PostMapping("/suggest-options")
    public ApiResult<SuggestOptionsResponse> suggestOptions(@Valid @RequestBody SuggestOptionsRequest request) {
        return ApiResult.ok(aiChatService.suggestOptions(request));
    }

    @PostMapping("/random-pick")
    public ApiResult<RandomPickResponse> randomPick(@Valid @RequestBody RandomPickRequest request) {
        return ApiResult.ok(aiChatService.randomPick(request));
    }
}

// 页面访问示例：
// 1. 首次对话（不传 conversationId，响应里会返回新的 id）
// POST http://localhost:8080/soulfood/ai/chat  {"message":"我喜欢吃辣"}
// 2. 继续对话（带上 conversationId）
// POST http://localhost:8080/soulfood/ai/chat  {"conversationId":"xxx","message":"刚才说的再推荐一道"}
// 3. 查看会话历史
// GET  http://localhost:8080/soulfood/ai/chat/{conversationId}/history
// 4. 清空会话记忆
// DELETE http://localhost:8080/soulfood/ai/chat/{conversationId}
