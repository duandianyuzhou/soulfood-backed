package com.food.soulfoodbackend.dto.favorite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddFavoriteRequest {

    @NotBlank
    private String targetType;

    private Long targetId;

    @NotBlank
    @Size(max = 128)
    private String title;

    @Size(max = 256)
    private String subtitle;

    private String category;
    private String score;
    private String duration;

    /** AI 回复全文（targetType=ai_reply 时使用） */
    private String content;

    /** AI 会话 ID（可选） */
    private String conversationId;
}
