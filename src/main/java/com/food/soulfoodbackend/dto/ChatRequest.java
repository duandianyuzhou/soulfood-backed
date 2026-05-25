package com.food.soulfoodbackend.dto;

public record ChatRequest(
        String conversationId,
        String message,
        Double lat,
        Double lng,
        String imageBase64,
        String imageMimeType) {
}
