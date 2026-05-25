package com.food.soulfoodbackend.dto;

import java.util.List;

public record ChatRequest(String conversationId, String message, Double lat, Double lng) {
}
