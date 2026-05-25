package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecommendRecipesResponse {

    private String conversationId;
    private List<RecipeItemDto> items;
    private String rawReply;
}
