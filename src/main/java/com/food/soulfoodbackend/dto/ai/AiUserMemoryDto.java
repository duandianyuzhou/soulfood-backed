package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class AiUserMemoryDto {

    private Long id;
    private String memoryType;
    private String content;
    private String source;
    private OffsetDateTime updatedAt;
}
