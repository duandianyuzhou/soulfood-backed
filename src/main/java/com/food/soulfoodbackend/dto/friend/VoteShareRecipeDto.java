package com.food.soulfoodbackend.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteShareRecipeDto {

    private String name;
    private String score;
    private String reason;
}
