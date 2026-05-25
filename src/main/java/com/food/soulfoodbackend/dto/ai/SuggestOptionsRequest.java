package com.food.soulfoodbackend.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class SuggestOptionsRequest {

    private String topic;
    private List<String> existingOptions;
}
