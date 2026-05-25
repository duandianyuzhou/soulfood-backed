package com.food.soulfoodbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AiMemoryExtractionService {

    private final AiUserMemoryService memoryService;
    private final ObjectMapper objectMapper;
    private final ChatClient statelessChatClient;

    @Value("${app.ai.memory-extraction-enabled:true}")
    private boolean extractionEnabled;

    public AiMemoryExtractionService(
            AiUserMemoryService memoryService,
            ObjectMapper objectMapper,
            @Qualifier("statelessChatClient") ChatClient statelessChatClient) {
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
        this.statelessChatClient = statelessChatClient;
    }

    public void scheduleExtraction(
            Long userId, String conversationId, String userMessage, String assistantReply) {
        if (!extractionEnabled || userId == null || !StringUtils.hasText(userMessage)) {
            return;
        }
        CompletableFuture.runAsync(() -> extract(userId, conversationId, userMessage, assistantReply));
    }

    private void extract(Long userId, String conversationId, String userMessage, String assistantReply) {
        try {
            String prompt = """
                    分析以下美食助手对话，提取值得长期记住的用户饮食事实（口味、习惯、过敏、忌口、常选菜系等）。
                    只提取用户明确表达的内容，不要猜测。若无新信息返回 {"memories":[]}
                    用户：%s
                    助手：%s
                    严格只输出 JSON：{"memories":["记忆1","记忆2"]}
                    """.formatted(
                    truncate(userMessage, 300),
                    truncate(assistantReply != null ? assistantReply : "", 400));
            String raw = statelessChatClient.prompt().user(prompt).call().content();
            List<String> memories = parseMemories(raw);
            if (!memories.isEmpty()) {
                memoryService.addExtractedMemories(userId, memories, "conversation", conversationId);
                log.debug("Extracted {} memories for user {}", memories.size(), userId);
            }
        } catch (Exception ex) {
            log.warn("AI memory extraction failed: {}", ex.getMessage());
        }
    }

    private List<String> parseMemories(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            String json = extractJson(raw);
            JsonNode node = objectMapper.readTree(json);
            JsonNode arr = node.get("memories");
            if (arr == null || !arr.isArray()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (JsonNode item : arr) {
                String text = item.asText("").trim();
                if (StringUtils.hasText(text) && text.length() >= 2) {
                    result.add(text);
                }
            }
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private static String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }
}
