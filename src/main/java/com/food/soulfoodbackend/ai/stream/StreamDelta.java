package com.food.soulfoodbackend.ai.stream;

public final class StreamDelta {

    private StreamDelta() {
    }

    /**
     * Ollama / 部分网关会推「累计全文」而不是增量。若本次内容已包含已输出部分，只取后缀，避免同一段回答被拼两次。
     */
    public static String of(String alreadyEmitted, String incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return "";
        }
        if (alreadyEmitted == null || alreadyEmitted.isEmpty()) {
            return incoming;
        }
        if (incoming.equals(alreadyEmitted)) {
            return "";
        }
        if (incoming.startsWith(alreadyEmitted)) {
            return incoming.substring(alreadyEmitted.length());
        }
        return incoming;
    }
}
