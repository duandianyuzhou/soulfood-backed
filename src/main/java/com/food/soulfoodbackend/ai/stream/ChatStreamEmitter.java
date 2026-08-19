package com.food.soulfoodbackend.ai.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatStreamEmitter {

    private final ObjectMapper objectMapper;

    public String workflow(String runId, String title, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "workflow");
        payload.put("runId", runId);
        payload.put("title", title);
        payload.put("status", status);
        return json(payload);
    }

    public String step(String runId, String id, String title, String status, String summary) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "step");
        payload.put("runId", runId);
        payload.put("id", id);
        payload.put("title", title);
        payload.put("status", status);
        if (summary != null) {
            payload.put("summary", summary);
        }
        return json(payload);
    }

    public String chunk(String text) {
        return json(Map.of("type", "chunk", "text", text == null ? "" : text));
    }

    public String cards(List<ChatActionCardDto> items) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "cards");
        payload.put("items", items == null ? List.of() : items);
        return json(payload);
    }

    public String done(String conversationId, List<ChatActionCardDto> cards) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "done");
        payload.put("conversationId", conversationId);
        payload.put("cards", cards == null ? List.of() : cards);
        return json(payload);
    }

    private String json(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload) + "\n";
        } catch (JsonProcessingException ex) {
            return "{\"type\":\"error\",\"message\":\"序列化失败\"}\n";
        }
    }
}
