package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.auth.LoginResponse;
import com.food.soulfoodbackend.dto.auth.MockLoginRequest;
import com.food.soulfoodbackend.dto.auth.UserProfileResponse;
import com.food.soulfoodbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/mock")
    public ApiResult<LoginResponse> mockLogin(@Valid @RequestBody MockLoginRequest request) {
        return ApiResult.ok(authService.mockLogin(request));
    }

    @GetMapping("/me")
    public ApiResult<UserProfileResponse> me() {
        return ApiResult.ok(authService.getProfile(UserContext.requireUserId()));
    }
}
