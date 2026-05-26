package com.food.soulfoodbackend.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendChatMessageDto {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String messageType;
    private String content;
    private String createdAt;
    private boolean mine;
    private VoteSharePayloadDto voteShare;
}
