package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_friend_conversation_read")
public class SfFriendConversationRead {

    private Long conversationId;
    private Long userId;
    private Long lastReadMessageId;
    private OffsetDateTime updatedAt;
}
