package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.food.soulfoodbackend.dto.friend.VoteSharePayloadDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FriendChatService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final String TYPE_TEXT = "text";
    private static final String TYPE_VOTE_SHARE = "vote_share";

    private final SfFriendConversationMapper conversationMapper;
    private final SfFriendMessageMapper messageMapper;
    private final SfFriendConversationReadMapper readMapper;
    private final SfFriendMapper friendMapper;
    private final SfUserMapper userMapper;
    private final FriendChatBroadcastService broadcastService;
    private final ObjectMapper objectMapper;

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
                    previewText(lastMessage),
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
                previewText(lastMessage),
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

        String type = normalizeMessageType(request.getMessageType());
        if (TYPE_VOTE_SHARE.equals(type)) {
            return persistAndBroadcast(conversation, userId, buildVoteShareMessage(request.getVoteShare()));
        }
        return persistAndBroadcast(conversation, userId, buildTextMessage(request.getContent()));
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

    private SfFriendMessage buildTextMessage(String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息不能为空");
        }
        SfFriendMessage message = new SfFriendMessage();
        message.setMessageType(TYPE_TEXT);
        message.setContent(content);
        return message;
    }

    private SfFriendMessage buildVoteShareMessage(VoteSharePayloadDto payload) {
        if (payload == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分享卡数据不能为空");
        }
        if (payload.getWinnerTitle() == null || payload.getWinnerTitle().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "胜出选项不能为空");
        }
        if (payload.getRecipes() == null) {
            payload.setRecipes(List.of());
        }
        if (payload.getVoteCount() == null) {
            payload.setVoteCount(0);
        }
        if (payload.getPercent() == null) {
            payload.setPercent(0);
        }
        if (payload.getGhoul() == null) {
            payload.setGhoul(false);
        }

        SfFriendMessage message = new SfFriendMessage();
        message.setMessageType(TYPE_VOTE_SHARE);
        message.setContent(previewVoteShare(payload));
        try {
            message.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分享卡数据格式错误");
        }
        return message;
    }

    private FriendChatMessageDto persistAndBroadcast(SfFriendConversation conversation, Long userId, SfFriendMessage message) {
        OffsetDateTime now = OffsetDateTime.now();
        message.setConversationId(conversation.getId());
        message.setSenderId(userId);
        message.setCreatedAt(now);
        messageMapper.insert(message);

        conversation.setUpdatedAt(now);
        conversationMapper.updateById(conversation);

        FriendChatMessageDto dto = toMessageDto(message, userId);
        broadcastService.broadcastMessage(conversation.getId(), dto);
        return dto;
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
        String type = message.getMessageType() == null ? TYPE_TEXT : message.getMessageType();
        VoteSharePayloadDto voteShare = null;
        if (TYPE_VOTE_SHARE.equals(type) && message.getPayloadJson() != null && !message.getPayloadJson().isBlank()) {
            try {
                voteShare = objectMapper.readValue(message.getPayloadJson(), VoteSharePayloadDto.class);
            } catch (JsonProcessingException ignored) {
                // ignore malformed payload
            }
        }
        return new FriendChatMessageDto(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                type,
                message.getContent(),
                formatTime(message.getCreatedAt()),
                Objects.equals(message.getSenderId(), userId),
                voteShare
        );
    }

    private static String previewText(SfFriendMessage message) {
        if (message == null) {
            return "";
        }
        if (TYPE_VOTE_SHARE.equals(message.getMessageType())) {
            return message.getContent() != null ? message.getContent() : "[投票结果]";
        }
        return message.getContent() == null ? "" : message.getContent();
    }

    private static String previewVoteShare(VoteSharePayloadDto payload) {
        return "[投票结果] " + payload.getWinnerTitle().trim();
    }

    private static String normalizeMessageType(String raw) {
        if (raw == null || raw.isBlank()) {
            return TYPE_TEXT;
        }
        String type = raw.trim().toLowerCase();
        if (TYPE_VOTE_SHARE.equals(type)) {
            return TYPE_VOTE_SHARE;
        }
        return TYPE_TEXT;
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
