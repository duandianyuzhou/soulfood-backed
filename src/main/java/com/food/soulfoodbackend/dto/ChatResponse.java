package com.food.soulfoodbackend.dto;

import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import com.food.soulfoodbackend.dto.ai.WorkflowSnapshotDto;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String reply,
        List<ChatActionCardDto> cards,
        WorkflowSnapshotDto workflow) {

    public ChatResponse(String conversationId, String reply, List<ChatActionCardDto> cards) {
        this(conversationId, reply, cards, null);
    }
}
