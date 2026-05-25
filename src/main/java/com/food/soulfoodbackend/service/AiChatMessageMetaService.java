package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.domain.entity.SfAiChatMessage;
import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
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
    public void saveCardsOnLatestAssistant(String conversationId, List<ChatActionCardDto> cards) {
        if (conversationId == null || conversationId.isBlank() || cards == null || cards.isEmpty()) {
            return;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            if (saveCardsOnce(conversationId, cards)) {
                return;
            }
            try {
                Thread.sleep(50L * (attempt + 1));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Failed to save chat cards after retries for conversation {}", conversationId);
    }

    private boolean saveCardsOnce(String conversationId, List<ChatActionCardDto> cards) {
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
            meta.put("cards", cards);
            row.setMetaJson(objectMapper.writeValueAsString(meta));
            messageMapper.updateById(row);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to save chat cards meta: {}", ex.getMessage());
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
}
