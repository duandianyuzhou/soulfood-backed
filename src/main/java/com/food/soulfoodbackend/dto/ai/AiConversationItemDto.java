package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class AiConversationItemDto {

    private String id;
    private String title;
    private String preview;
    private String sceneTag;
    private OffsetDateTime updatedAt;
}
