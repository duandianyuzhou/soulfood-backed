package com.food.soulfoodbackend.dto;

import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;

import java.util.List;

public record ChatResponse(String conversationId, String reply, List<ChatActionCardDto> cards) {
}
