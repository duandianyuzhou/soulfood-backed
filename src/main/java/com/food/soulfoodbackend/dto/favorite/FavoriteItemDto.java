package com.food.soulfoodbackend.dto.favorite;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FavoriteItemDto {

    private Long id;
    private String targetType;
    private Long targetId;
    private String title;
    private String subtitle;
    private String category;
    private String score;
    private String duration;
    private String content;
    private String conversationId;
}
