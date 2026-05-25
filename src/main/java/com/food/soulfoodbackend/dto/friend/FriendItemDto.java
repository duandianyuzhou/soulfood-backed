package com.food.soulfoodbackend.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FriendItemDto {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String source;
    private String addedAt;
}
