package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.domain.entity.SfAiChatMessage;
import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import com.food.soulfoodbackend.dto.ai.WorkflowSnapshotDto;
import com.food.soulfoodbackend.mapper.SfAiChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatMessageMetaService {

    private static final TypeReference<List<ChatActionCardDto>> CARD_LIST_TYPE = new TypeReference<>() {
    };

    private final SfAiChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveOnLatestAssistant(
            String conversationId,
            List<ChatActionCardDto> cards,
            WorkflowSnapshotDto workflow) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        if ((cards == null || cards.isEmpty()) && workflow == null) {
            return;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            if (saveOnce(conversationId, cards, workflow)) {
                return;
            }
            try {
                Thread.sleep(50L * (attempt + 1));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Failed to save chat meta after retries for conversation {}", conversationId);
    }

    @Transactional
    public void saveCardsOnLatestAssistant(String conversationId, List<ChatActionCardDto> cards) {
        saveOnLatestAssistant(conversationId, cards, null);
    }

    private boolean saveOnce(
            String conversationId,
            List<ChatActionCardDto> cards,
            WorkflowSnapshotDto workflow) {
        SfAiChatMessage row = messageMapper.selectOne(new LambdaQueryWrapper<SfAiChatMessage>()
                .eq(SfAiChatMessage::getConversationId, conversationId)
                .eq(SfAiChatMessage::getMessageType, "ASSISTANT")
                .orderByDesc(SfAiChatMessage::getSortOrder)
                .orderByDesc(SfAiChatMessage::getId)
                .last("LIMIT 1"));
        if (row == null) {
            return false;
        }
        try {
            Map<String, Object> meta = new HashMap<>();
            if (cards != null && !cards.isEmpty()) {
                meta.put("cards", cards);
            }
            if (workflow != null) {
                meta.put("workflow", workflow);
            }
            row.setMetaJson(objectMapper.writeValueAsString(meta));
            messageMapper.updateById(row);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to save chat meta: {}", ex.getMessage());
            return false;
        }
    }

    public List<ChatActionCardDto> parseCards(String metaJson) {
        if (metaJson == null || metaJson.isBlank()) {
            return List.of();
        }
        try {
            var node = objectMapper.readTree(metaJson);
            var cardsNode = node.get("cards");
            if (cardsNode == null || !cardsNode.isArray()) {
                return List.of();
            }
            return objectMapper.convertValue(cardsNode, CARD_LIST_TYPE);
        } catch (Exception ex) {
            return List.of();
        }
    }

    public WorkflowSnapshotDto parseWorkflow(String metaJson) {
        if (metaJson == null || metaJson.isBlank()) {
            return null;
        }
        try {
            var node = objectMapper.readTree(metaJson);
            var workflowNode = node.get("workflow");
            if (workflowNode == null || workflowNode.isNull()) {
                return null;
            }
            return objectMapper.convertValue(workflowNode, WorkflowSnapshotDto.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
