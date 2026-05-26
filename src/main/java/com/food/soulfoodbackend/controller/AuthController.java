package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.auth.ChangePasswordRequest;
import com.food.soulfoodbackend.dto.auth.LoginResponse;
import com.food.soulfoodbackend.dto.auth.MockLoginRequest;
import com.food.soulfoodbackend.dto.auth.PasswordLoginRequest;
import com.food.soulfoodbackend.dto.auth.RegisterRequest;
import com.food.soulfoodbackend.dto.auth.UpdateProfileRequest;
import com.food.soulfoodbackend.dto.auth.UserProfileResponse;
import com.food.soulfoodbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 开发调试用，正式环境可关闭 */
    @PostMapping("/login/mock")
    public ApiResult<LoginResponse> mockLogin(@Valid @RequestBody MockLoginRequest request) {
        return ApiResult.ok(authService.mockLogin(request));
    }

    @PostMapping("/register")
    public ApiResult<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody PasswordLoginRequest request) {
        return ApiResult.ok(authService.loginWithPassword(request));
    }

    @GetMapping("/me")
    public ApiResult<UserProfileResponse> me() {
        return ApiResult.ok(authService.getProfile(UserContext.requireUserId()));
    }

    @PatchMapping("/me")
    public ApiResult<UserProfileResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResult.ok(authService.updateProfile(UserContext.requireUserId(), request));
    }

    @PostMapping("/me/password")
    public ApiResult<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(UserContext.requireUserId(), request);
        return ApiResult.ok(null);
    }
}
