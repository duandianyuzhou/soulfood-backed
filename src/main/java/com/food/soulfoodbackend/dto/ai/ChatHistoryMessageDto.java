package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryMessageDto {

    private String role;
    private String content;
    private List<ChatActionCardDto> cards;
    private WorkflowSnapshotDto workflow;
}
