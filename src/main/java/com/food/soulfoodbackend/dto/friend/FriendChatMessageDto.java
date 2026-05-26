package com.food.soulfoodbackend.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FriendChatMessageDto {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private String createdAt;
    private boolean mine;
}
