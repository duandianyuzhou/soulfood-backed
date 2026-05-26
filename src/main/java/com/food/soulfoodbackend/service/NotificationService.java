package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.domain.entity.SfRoom;
import com.food.soulfoodbackend.domain.entity.SfRoomOption;
import com.food.soulfoodbackend.domain.entity.SfUserNotification;
import com.food.soulfoodbackend.domain.entity.SfVote;
import com.food.soulfoodbackend.dto.notification.NotificationDto;
import com.food.soulfoodbackend.dto.notification.NotificationListResponse;
import com.food.soulfoodbackend.mapper.SfUserNotificationMapper;
import com.food.soulfoodbackend.mapper.SfVoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SfUserNotificationMapper notificationMapper;
    private final SfVoteMapper voteMapper;
    private final ObjectMapper objectMapper;

    public NotificationListResponse list(Long userId) {
        List<SfUserNotification> rows = notificationMapper.selectList(new LambdaQueryWrapper<SfUserNotification>()
                .eq(SfUserNotification::getUserId, userId)
                .orderByDesc(SfUserNotification::getCreatedAt)
                .last("LIMIT 50"));
        int unread = Math.toIntExact(notificationMapper.selectCount(new LambdaQueryWrapper<SfUserNotification>()
                .eq(SfUserNotification::getUserId, userId)
                .eq(SfUserNotification::getRead, false)));
        List<NotificationDto> items = rows.stream().map(this::toDto).toList();
        return new NotificationListResponse(unread, items);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<SfUserNotification>()
                .eq(SfUserNotification::getId, notificationId)
                .eq(SfUserNotification::getUserId, userId)
                .set(SfUserNotification::getRead, true));
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<SfUserNotification>()
                .eq(SfUserNotification::getUserId, userId)
                .eq(SfUserNotification::getRead, false)
                .set(SfUserNotification::getRead, true));
    }

    @Transactional
    public void notifyRoomClosed(SfRoom room, SfRoomOption winner) {
        Set<Long> userIds = new LinkedHashSet<>();
        List<SfVote> votes = voteMapper.selectList(new LambdaQueryWrapper<SfVote>()
                .eq(SfVote::getRoomId, room.getId()));
        for (SfVote vote : votes) {
            userIds.add(vote.getUserId());
        }
        if (room.getOwnerId() != null) {
            userIds.add(room.getOwnerId());
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "roomCode", room.getCode(),
                    "winnerTitle", winner.getTitle() == null ? "" : winner.getTitle()));
        } catch (JsonProcessingException ex) {
            payload = "{\"roomCode\":\"" + room.getCode() + "\"}";
        }

        String body = "「" + winner.getTitle() + "」胜出，点击查看投票结果与菜谱推荐。";
        for (Long userId : userIds) {
            SfUserNotification row = new SfUserNotification();
            row.setUserId(userId);
            row.setType("room_closed");
            row.setTitle("投票已结束：" + room.getTopic());
            row.setBody(body);
            row.setPayloadJson(payload);
            row.setRead(false);
            row.setCreatedAt(OffsetDateTime.now());
            row.setDeleted(false);
            notificationMapper.insert(row);
        }
    }

    private NotificationDto toDto(SfUserNotification row) {
        String roomCode = null;
        if (row.getPayloadJson() != null && !row.getPayloadJson().isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = objectMapper.readValue(row.getPayloadJson(), Map.class);
                Object code = payload.get("roomCode");
                if (code != null) {
                    roomCode = code.toString();
                }
            } catch (JsonProcessingException ignored) {
                // ignore malformed payload
            }
        }
        return new NotificationDto(
                row.getId(),
                row.getType(),
                row.getTitle(),
                row.getBody(),
                roomCode,
                Boolean.TRUE.equals(row.getRead()),
                row.getCreatedAt() == null ? "" : DISPLAY_TIME.format(row.getCreatedAt()));
    }
}
