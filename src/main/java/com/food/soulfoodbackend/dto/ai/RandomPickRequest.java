package com.food.soulfoodbackend.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class RandomPickRequest {

    private List<String> candidates;
    private String preference;
}
