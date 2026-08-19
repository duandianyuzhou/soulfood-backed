package com.food.soulfoodbackend.ai.router;

public record RouteDecision(ChatIntent intent, String source, double confidence) {

    public static RouteDecision of(ChatIntent intent, String source) {
        return new RouteDecision(intent, source, 1.0);
    }
}
