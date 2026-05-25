package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChatHistoryMessageDto {

    private String role;
    private String content;
    private List<ChatActionCardDto> cards;
}
