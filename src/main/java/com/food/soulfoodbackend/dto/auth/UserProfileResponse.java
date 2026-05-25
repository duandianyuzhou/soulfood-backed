package com.food.soulfoodbackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String signature;
}
