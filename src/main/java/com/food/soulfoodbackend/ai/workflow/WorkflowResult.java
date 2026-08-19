package com.food.soulfoodbackend.ai.workflow;

import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import com.food.soulfoodbackend.dto.ai.WorkflowSnapshotDto;

import java.util.List;

public record WorkflowResult(
        List<String> events,
        String reply,
        List<ChatActionCardDto> cards,
        WorkflowSnapshotDto snapshot,
        boolean waiting) {
}
