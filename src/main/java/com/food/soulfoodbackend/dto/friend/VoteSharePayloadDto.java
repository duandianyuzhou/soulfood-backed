package com.food.soulfoodbackend.dto.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VoteSharePayloadDto {

    private String roomCode;
    private String roomTopic;

    @NotBlank(message = "胜出选项不能为空")
    @Size(max = 120)
    private String winnerTitle;

    private Integer voteCount;
    private Integer percent;
    private String preferenceText;
    private Boolean ghoul;
    private List<VoteShareRecipeDto> recipes = new ArrayList<>();
}
