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
        return listForUser(userId, null, null);
    }

    public List<AiConversationItemDto> listForUser(Long userId, String keyword, String sceneTag) {
        if (userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<SfAiConversation> wrapper = new LambdaQueryWrapper<SfAiConversation>()
                .eq(SfAiConversation::getUserId, userId);
        if (sceneTag != null && !sceneTag.isBlank()) {
            wrapper.eq(SfAiConversation::getSceneTag, sceneTag.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            List<String> matchedIds = findConversationIdsByMessageContent(userId, kw);
            wrapper.and(w -> w.like(SfAiConversation::getTitle, kw)
                    .or()
                    .in(!matchedIds.isEmpty(), SfAiConversation::getId, matchedIds));
        }
        wrapper.orderByDesc(SfAiConversation::getUpdatedAt);
        List<SfAiConversation> rows = conversationMapper.selectList(wrapper);
        List<AiConversationItemDto> result = new ArrayList<>();
        for (SfAiConversation row : rows) {
            String preview = loadPreview(row.getId());
            String title = row.getTitle();
            if (title == null || title.isBlank()) {
                title = preview.isBlank() ? "新对话" : truncate(preview, TITLE_MAX);
            }
            result.add(new AiConversationItemDto(
                    row.getId(), title, preview, row.getSceneTag(), row.getUpdatedAt()));
        }
        return result;
    }

    @Transactional
    public void updateConversation(String conversationId, Long userId, String title, String sceneTag) {
        assertOwnedByUser(conversationId, userId);
        SfAiConversation row = conversationMapper.selectById(conversationId);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        boolean changed = false;
        if (title != null && !title.isBlank()) {
            row.setTitle(truncate(title.trim(), TITLE_MAX));
            changed = true;
        }
        if (sceneTag != null) {
            row.setSceneTag(sceneTag.isBlank() ? null : sceneTag.trim());
            changed = true;
        }
        if (changed) {
            row.setUpdatedAt(OffsetDateTime.now());
            conversationMapper.updateById(row);
        }
    }

    private List<String> findConversationIdsByMessageContent(Long userId, String keyword) {
        List<SfAiConversation> owned = conversationMapper.selectList(new LambdaQueryWrapper<SfAiConversation>()
                .eq(SfAiConversation::getUserId, userId)
                .select(SfAiConversation::getId));
        if (owned.isEmpty()) {
            return List.of();
        }
        List<String> ids = owned.stream().map(SfAiConversation::getId).toList();
        return messageMapper.selectList(new LambdaQueryWrapper<SfAiChatMessage>()
                        .in(SfAiChatMessage::getConversationId, ids)
                        .like(SfAiChatMessage::getContent, keyword)
                        .select(SfAiChatMessage::getConversationId))
                .stream()
                .map(SfAiChatMessage::getConversationId)
                .distinct()
                .toList();
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
