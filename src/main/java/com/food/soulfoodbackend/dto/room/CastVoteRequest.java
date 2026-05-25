package com.food.soulfoodbackend.dto.room;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CastVoteRequest {

    @NotNull
    private Long optionId;
}
