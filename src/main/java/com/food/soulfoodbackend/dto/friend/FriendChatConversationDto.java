package com.food.soulfoodbackend.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FriendChatConversationDto {

    private Long conversationId;
    private Long peerUserId;
    private String peerNickname;
    private String peerAvatarUrl;
    private String lastMessage;
    private String lastMessageAt;
    private int unreadCount;
}
