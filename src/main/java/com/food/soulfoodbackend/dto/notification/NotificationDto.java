package com.food.soulfoodbackend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private String type;
    private String title;
    private String body;
    private String roomCode;
    private boolean read;
    private String createdAt;
}
