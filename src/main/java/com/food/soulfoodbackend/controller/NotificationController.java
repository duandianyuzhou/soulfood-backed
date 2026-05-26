package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.notification.NotificationListResponse;
import com.food.soulfoodbackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResult<NotificationListResponse> list() {
        return ApiResult.ok(notificationService.list(UserContext.requireUserId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResult<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(UserContext.requireUserId(), id);
        return ApiResult.ok(null);
    }

    @PostMapping("/read-all")
    public ApiResult<Void> markAllRead() {
        notificationService.markAllRead(UserContext.requireUserId());
        return ApiResult.ok(null);
    }
}
