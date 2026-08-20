package com.food.soulfoodbackend.ai;

final class CapacityExceeded {

    private CapacityExceeded() {}

    static boolean of(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && looksLikeOverload(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean looksLikeOverload(String message) {
        String text = message.toLowerCase();
        return text.contains("429")
                || text.contains("\"code\":\"1305\"")
                || text.contains("code\":1305")
                || text.contains("访问量过大")
                || text.contains("too many requests")
                || text.contains("rate limit")
                || text.contains("overloaded")
                || text.contains("capacity");
    }
}
