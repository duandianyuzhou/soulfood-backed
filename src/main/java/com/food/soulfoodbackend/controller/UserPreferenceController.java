package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.preference.PreferenceResponse;
import com.food.soulfoodbackend.dto.preference.UpdatePreferenceRequest;
import com.food.soulfoodbackend.service.UserPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @GetMapping
    public ApiResult<PreferenceResponse> getPreference() {
        return ApiResult.ok(userPreferenceService.getPreference(UserContext.requireUserId()));
    }

    @PutMapping
    public ApiResult<PreferenceResponse> updatePreference(@Valid @RequestBody UpdatePreferenceRequest request) {
        return ApiResult.ok(userPreferenceService.updatePreference(UserContext.requireUserId(), request));
    }
}
