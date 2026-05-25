package com.food.soulfoodbackend.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.domain.entity.SfAiChatMessage;
import com.food.soulfoodbackend.domain.entity.SfAiConversation;
import com.food.soulfoodbackend.mapper.SfAiChatMessageMapper;
import com.food.soulfoodbackend.mapper.SfAiConversationMapper;
import com.food.soulfoodbackend.service.AiConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PgChatMemoryRepository implements ChatMemoryRepository {

    private final SfAiConversationMapper conversationMapper;
    private final SfAiChatMessageMapper messageMapper;
    private final AiConversationService conversationService;

    @Override
    public List<String> findConversationIds() {
        return conversationMapper.selectList(new LambdaQueryWrapper<SfAiConversation>()
                        .orderByDesc(SfAiConversation::getUpdatedAt))
                .stream()
                .map(SfAiConversation::getId)
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<SfAiChatMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<SfAiChatMessage>()
                .eq(SfAiChatMessage::getConversationId, conversationId)
                .orderByAsc(SfAiChatMessage::getSortOrder)
                .orderByAsc(SfAiChatMessage::getId));
        List<Message> messages = new ArrayList<>();
        for (SfAiChatMessage row : rows) {
            messages.add(toMessage(row));
        }
        return messages;
    }

    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        conversationService.ensureConversation(conversationId, null);

        messageMapper.delete(new LambdaQueryWrapper<SfAiChatMessage>()
                .eq(SfAiChatMessage::getConversationId, conversationId));

        int order = 0;
        OffsetDateTime now = OffsetDateTime.now();
        for (Message message : messages) {
            SfAiChatMessage row = new SfAiChatMessage();
            row.setConversationId(conversationId);
            row.setSortOrder(order++);
            row.setMessageType(message.getMessageType().name());
            row.setContent(message.getText() != null ? message.getText() : "");
            row.setCreatedAt(now);
            messageMapper.insert(row);
        }
        conversationService.touchConversation(conversationId);
    }

    @Override
    @Transactional
    public void deleteByConversationId(String conversationId) {
        conversationMapper.deleteById(conversationId);
    }

    private Message toMessage(SfAiChatMessage row) {
        MessageType type = MessageType.valueOf(row.getMessageType());
        String content = row.getContent() != null ? row.getContent() : "";
        return switch (type) {
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            case TOOL -> new AssistantMessage(content);
            default -> new UserMessage(content);
        };
    }
}
