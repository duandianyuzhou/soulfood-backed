package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfFriend;
import com.food.soulfoodbackend.domain.entity.SfFriendConversation;
import com.food.soulfoodbackend.domain.entity.SfFriendConversationRead;
import com.food.soulfoodbackend.domain.entity.SfFriendMessage;
import com.food.soulfoodbackend.domain.entity.SfUser;
import com.food.soulfoodbackend.dto.friend.FriendChatConversationDto;
import com.food.soulfoodbackend.dto.friend.FriendChatMessageDto;
import com.food.soulfoodbackend.dto.friend.SendFriendMessageRequest;
import com.food.soulfoodbackend.friend.ws.FriendChatBroadcastService;
import com.food.soulfoodbackend.mapper.SfFriendConversationMapper;
import com.food.soulfoodbackend.mapper.SfFriendConversationReadMapper;
import com.food.soulfoodbackend.mapper.SfFriendMapper;
import com.food.soulfoodbackend.mapper.SfFriendMessageMapper;
import com.food.soulfoodbackend.mapper.SfUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FriendChatService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final SfFriendConversationMapper conversationMapper;
    private final SfFriendMessageMapper messageMapper;
    private final SfFriendConversationReadMapper readMapper;
    private final SfFriendMapper friendMapper;
    private final SfUserMapper userMapper;
    private final FriendChatBroadcastService broadcastService;

    public List<FriendChatConversationDto> listConversations(Long userId) {
        List<SfFriendConversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<SfFriendConversation>()
                        .and(w -> w.eq(SfFriendConversation::getUserLowId, userId)
                                .or()
                                .eq(SfFriendConversation::getUserHighId, userId))
                        .orderByDesc(SfFriendConversation::getUpdatedAt));

        List<FriendChatConversationDto> result = new ArrayList<>();
        for (SfFriendConversation conversation : conversations) {
            Long peerUserId = peerUserId(conversation, userId);
            SfUser peer = userMapper.selectById(peerUserId);
            if (peer == null) {
                continue;
            }
            SfFriendMessage lastMessage = latestMessage(conversation.getId());
            long unread = countUnread(conversation.getId(), userId);
            result.add(new FriendChatConversationDto(
                    conversation.getId(),
                    peer.getId(),
                    peer.getNickname(),
                    peer.getAvatarUrl(),
                    lastMessage == null ? "" : lastMessage.getContent(),
                    lastMessage == null ? "" : formatTime(lastMessage.getCreatedAt()),
                    (int) unread
            ));
        }
        return result;
    }

    @Transactional
    public FriendChatConversationDto openConversation(Long userId, Long friendUserId) {
        assertAreFriends(userId, friendUserId);
        SfFriendConversation conversation = findOrCreateConversation(userId, friendUserId);
        Long peerUserId = peerUserId(conversation, userId);
        SfUser peer = userMapper.selectById(peerUserId);
        if (peer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "好友不存在");
        }
        SfFriendMessage lastMessage = latestMessage(conversation.getId());
        return new FriendChatConversationDto(
                conversation.getId(),
                peer.getId(),
                peer.getNickname(),
                peer.getAvatarUrl(),
                lastMessage == null ? "" : lastMessage.getContent(),
                lastMessage == null ? "" : formatTime(lastMessage.getCreatedAt()),
                (int) countUnread(conversation.getId(), userId)
        );
    }

    public List<FriendChatMessageDto> listMessages(Long userId, Long conversationId, Long beforeId, Integer limit) {
        SfFriendConversation conversation = requireParticipantConversation(conversationId, userId);
        int pageSize = normalizeLimit(limit);

        LambdaQueryWrapper<SfFriendMessage> query = new LambdaQueryWrapper<SfFriendMessage>()
                .eq(SfFriendMessage::getConversationId, conversation.getId())
                .orderByDesc(SfFriendMessage::getId)
                .last("LIMIT " + pageSize);
        if (beforeId != null && beforeId > 0) {
            query.lt(SfFriendMessage::getId, beforeId);
        }

        List<SfFriendMessage> rows = messageMapper.selectList(query);
        List<FriendChatMessageDto> result = new ArrayList<>(rows.size());
        for (int i = rows.size() - 1; i >= 0; i--) {
            result.add(toMessageDto(rows.get(i), userId));
        }
        return result;
    }

    @Transactional
    public FriendChatMessageDto sendMessage(Long userId, Long conversationId, SendFriendMessageRequest request) {
        SfFriendConversation conversation = requireParticipantConversation(conversationId, userId);
        Long peerUserId = peerUserId(conversation, userId);
        assertAreFriends(userId, peerUserId);

        String content = request.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息不能为空");
        }

        OffsetDateTime now = OffsetDateTime.now();
        SfFriendMessage message = new SfFriendMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(userId);
        message.setContent(content);
        message.setCreatedAt(now);
        messageMapper.insert(message);

        conversation.setUpdatedAt(now);
        conversationMapper.updateById(conversation);

        FriendChatMessageDto dto = toMessageDto(message, userId);
        broadcastService.broadcastMessage(conversation.getId(), dto);
        return dto;
    }

    @Transactional
    public void markRead(Long userId, Long conversationId) {
        SfFriendConversation conversation = requireParticipantConversation(conversationId, userId);
        SfFriendMessage latest = latestMessage(conversation.getId());
        if (latest == null) {
            return;
        }
        upsertReadCursor(conversation.getId(), userId, latest.getId());
    }

    public void assertCanAccessConversation(Long userId, Long conversationId) {
        requireParticipantConversation(conversationId, userId);
    }

    private SfFriendConversation requireParticipantConversation(Long conversationId, Long userId) {
        SfFriendConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        if (!Objects.equals(conversation.getUserLowId(), userId)
                && !Objects.equals(conversation.getUserHighId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return conversation;
    }

    private SfFriendConversation findOrCreateConversation(Long userId, Long friendUserId) {
        long low = Math.min(userId, friendUserId);
        long high = Math.max(userId, friendUserId);
        SfFriendConversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<SfFriendConversation>()
                .eq(SfFriendConversation::getUserLowId, low)
                .eq(SfFriendConversation::getUserHighId, high));
        if (existing != null) {
            return existing;
        }
        OffsetDateTime now = OffsetDateTime.now();
        SfFriendConversation created = new SfFriendConversation();
        created.setUserLowId(low);
        created.setUserHighId(high);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        conversationMapper.insert(created);
        return created;
    }

    private void assertAreFriends(Long userId, Long friendUserId) {
        Long count = friendMapper.selectCount(new LambdaQueryWrapper<SfFriend>()
                .eq(SfFriend::getUserId, userId)
                .eq(SfFriend::getFriendUserId, friendUserId));
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能与好友聊天");
        }
    }

    private SfFriendMessage latestMessage(Long conversationId) {
        return messageMapper.selectOne(new LambdaQueryWrapper<SfFriendMessage>()
                .eq(SfFriendMessage::getConversationId, conversationId)
                .orderByDesc(SfFriendMessage::getId)
                .last("LIMIT 1"));
    }

    private long countUnread(Long conversationId, Long userId) {
        Long lastReadId = readCursor(conversationId, userId);
        LambdaQueryWrapper<SfFriendMessage> query = new LambdaQueryWrapper<SfFriendMessage>()
                .eq(SfFriendMessage::getConversationId, conversationId)
                .ne(SfFriendMessage::getSenderId, userId);
        if (lastReadId != null && lastReadId > 0) {
            query.gt(SfFriendMessage::getId, lastReadId);
        }
        Long count = messageMapper.selectCount(query);
        return count == null ? 0 : count;
    }

    private Long readCursor(Long conversationId, Long userId) {
        SfFriendConversationRead row = readMapper.selectOne(new LambdaQueryWrapper<SfFriendConversationRead>()
                .eq(SfFriendConversationRead::getConversationId, conversationId)
                .eq(SfFriendConversationRead::getUserId, userId));
        return row == null ? null : row.getLastReadMessageId();
    }

    private void upsertReadCursor(Long conversationId, Long userId, Long messageId) {
        SfFriendConversationRead existing = readMapper.selectOne(new LambdaQueryWrapper<SfFriendConversationRead>()
                .eq(SfFriendConversationRead::getConversationId, conversationId)
                .eq(SfFriendConversationRead::getUserId, userId));
        OffsetDateTime now = OffsetDateTime.now();
        if (existing == null) {
            SfFriendConversationRead created = new SfFriendConversationRead();
            created.setConversationId(conversationId);
            created.setUserId(userId);
            created.setLastReadMessageId(messageId);
            created.setUpdatedAt(now);
            readMapper.insert(created);
            return;
        }
        if (existing.getLastReadMessageId() != null && existing.getLastReadMessageId() >= messageId) {
            return;
        }
        readMapper.update(null, new LambdaUpdateWrapper<SfFriendConversationRead>()
                .eq(SfFriendConversationRead::getConversationId, conversationId)
                .eq(SfFriendConversationRead::getUserId, userId)
                .set(SfFriendConversationRead::getLastReadMessageId, messageId)
                .set(SfFriendConversationRead::getUpdatedAt, now));
    }

    private FriendChatMessageDto toMessageDto(SfFriendMessage message, Long userId) {
        return new FriendChatMessageDto(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getContent(),
                formatTime(message.getCreatedAt()),
                Objects.equals(message.getSenderId(), userId)
        );
    }

    private static Long peerUserId(SfFriendConversation conversation, Long userId) {
        return Objects.equals(conversation.getUserLowId(), userId)
                ? conversation.getUserHighId()
                : conversation.getUserLowId();
    }

    private static String formatTime(OffsetDateTime time) {
        return time == null ? "" : DISPLAY_TIME.format(time);
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
