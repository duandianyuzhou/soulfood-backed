package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.domain.entity.SfAiConversation;
import com.food.soulfoodbackend.mapper.SfAiConversationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AiConversationService {

    private final SfAiConversationMapper conversationMapper;

    @Transactional
    public void ensureConversation(String conversationId, Long userId) {
        SfAiConversation existing = conversationMapper.selectById(conversationId);
        if (existing == null) {
            SfAiConversation row = new SfAiConversation();
            row.setId(conversationId);
            row.setUserId(userId);
            row.setCreatedAt(OffsetDateTime.now());
            row.setUpdatedAt(OffsetDateTime.now());
            conversationMapper.insert(row);
            return;
        }
        if (userId != null && existing.getUserId() == null) {
            existing.setUserId(userId);
            existing.setUpdatedAt(OffsetDateTime.now());
            conversationMapper.updateById(existing);
        }
    }

    @Transactional
    public void touchConversation(String conversationId) {
        SfAiConversation row = conversationMapper.selectById(conversationId);
        if (row != null) {
            row.setUpdatedAt(OffsetDateTime.now());
            conversationMapper.updateById(row);
        }
    }
}
