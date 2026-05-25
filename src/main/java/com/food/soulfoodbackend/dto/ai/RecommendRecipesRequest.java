package com.food.soulfoodbackend.dto.ai;

import lombok.Data;

@Data
public class RecommendRecipesRequest {

    private String preference;
    private String voteWinner;
    private Integer participants;
    private String conversationId;
}
