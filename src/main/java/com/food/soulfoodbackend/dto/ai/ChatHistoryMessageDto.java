package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatHistoryMessageDto {

    private String role;
    private String content;
}
