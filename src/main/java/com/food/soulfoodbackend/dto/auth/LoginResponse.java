package com.food.soulfoodbackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private Long userId;
    private String token;
    private String nickname;
    private String avatarUrl;
}
