package com.food.soulfoodbackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockLoginRequest {

    @NotBlank
    private String deviceId;
    private String nickname;
}
