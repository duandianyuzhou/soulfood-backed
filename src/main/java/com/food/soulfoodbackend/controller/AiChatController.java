package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.dto.ChatRequest;
import com.food.soulfoodbackend.dto.ChatResponse;
import com.food.soulfoodbackend.service.AiChatService;
import org.springframework.ai.chat.messages.Message;
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
        String reply = aiChatService.chat(conversationId, request.message());
        return ApiResult.ok(new ChatResponse(conversationId, reply));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
        String resolvedId = aiChatService.resolveConversationId(conversationId);
        return aiChatService.chatStream(resolvedId, message);
    }

    @GetMapping("/chat/{conversationId}/history")
    public List<Message> history(@PathVariable String conversationId) {
        return aiChatService.getHistory(conversationId);
    }

    @DeleteMapping("/chat/{conversationId}")
    public Map<String, String> clearMemory(@PathVariable String conversationId) {
        aiChatService.clearMemory(conversationId);
        return Map.of("conversationId", conversationId, "status", "cleared");
    }

    @GetMapping("/recommend")
    public ApiResult<ChatResponse> recommend(@RequestParam(defaultValue = "清淡、少油、适合晚餐") String preference) {
        return ApiResult.ok(new ChatResponse(null, aiChatService.recommend(preference)));
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
