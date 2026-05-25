package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfAiChatMessage;
import com.food.soulfoodbackend.domain.entity.SfAiConversation;
import com.food.soulfoodbackend.dto.ai.AiConversationItemDto;
import com.food.soulfoodbackend.mapper.SfAiChatMessageMapper;
import com.food.soulfoodbackend.mapper.SfAiConversationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiConversationService {

    private static final int TITLE_MAX = 64;
    private static final int PREVIEW_MAX = 80;

    private final SfAiConversationMapper conversationMapper;
    private final SfAiChatMessageMapper messageMapper;

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

    @Transactional
    public void setTitleIfBlank(String conversationId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return;
        }
        SfAiConversation row = conversationMapper.selectById(conversationId);
        if (row == null || (row.getTitle() != null && !row.getTitle().isBlank())) {
            return;
        }
        row.setTitle(truncate(userMessage.trim(), TITLE_MAX));
        row.setUpdatedAt(OffsetDateTime.now());
        conversationMapper.updateById(row);
    }

    public List<AiConversationItemDto> listForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<SfAiConversation> rows = conversationMapper.selectList(new LambdaQueryWrapper<SfAiConversation>()
                .eq(SfAiConversation::getUserId, userId)
                .orderByDesc(SfAiConversation::getUpdatedAt));
        List<AiConversationItemDto> result = new ArrayList<>();
        for (SfAiConversation row : rows) {
            String preview = loadPreview(row.getId());
            String title = row.getTitle();
            if (title == null || title.isBlank()) {
                title = preview.isBlank() ? "新对话" : truncate(preview, TITLE_MAX);
            }
            result.add(new AiConversationItemDto(row.getId(), title, preview, row.getUpdatedAt()));
        }
        return result;
    }

    public void assertOwnedByUser(String conversationId, Long userId) {
        SfAiConversation row = conversationMapper.selectById(conversationId);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (userId != null && row.getUserId() != null && !userId.equals(row.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
    }

    private String loadPreview(String conversationId) {
        SfAiChatMessage last = messageMapper.selectOne(new LambdaQueryWrapper<SfAiChatMessage>()
                .eq(SfAiChatMessage::getConversationId, conversationId)
                .orderByDesc(SfAiChatMessage::getSortOrder)
                .orderByDesc(SfAiChatMessage::getId)
                .last("LIMIT 1"));
        if (last == null || last.getContent() == null) {
            return "";
        }
        return truncate(last.getContent().replace('\n', ' ').trim(), PREVIEW_MAX);
    }

    private static String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }
}
