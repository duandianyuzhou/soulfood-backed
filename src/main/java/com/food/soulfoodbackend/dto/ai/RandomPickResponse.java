package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RandomPickResponse {

    private String name;
    private String reason;
    private String rawReply;
}
