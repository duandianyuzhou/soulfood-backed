package com.food.soulfoodbackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordLoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
