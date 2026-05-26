package com.food.soulfoodbackend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class NotificationListResponse {

    private int unreadCount;
    private List<NotificationDto> items;
}
