package com.food.soulfoodbackend.ai.stream;

public final class StreamDelta {

    private StreamDelta() {
    }

    /**
     * Ollama / 部分网关会推「累计全文」而不是增量。若本次内容已包含已输出部分，只取后缀，避免同一段回答被拼两次。
     */
    public static String of(String alreadyEmitted, String incoming) {
        return of(alreadyEmitted, incoming, null);
    }

    /**
     * @param priorAssistant 上一轮助手全文。模型若把旧回答再打一遍，只保留新内容；对话历史仍可作为 prompt context。
     */
    public static String of(String alreadyEmitted, String incoming, String priorAssistant) {
        String payload = stripLeadingPrior(incoming, priorAssistant);
        if (payload.isEmpty()) {
            return "";
        }
        if (isIncompletePrior(payload, priorAssistant) && isBlank(alreadyEmitted)) {
            return "";
        }
        return cumulativeDelta(alreadyEmitted, payload);
    }

    private static String stripLeadingPrior(String incoming, String priorAssistant) {
        if (incoming == null || incoming.isEmpty()) {
            return "";
        }
        if (isBlank(priorAssistant)) {
            return incoming;
        }
        String prior = priorAssistant.trim();
        String text = incoming.stripLeading();
        if (text.startsWith(prior)) {
            return text.substring(prior.length()).stripLeading();
        }
        return incoming;
    }

    private static boolean isIncompletePrior(String payload, String priorAssistant) {
        if (isBlank(priorAssistant)) {
            return false;
        }
        String prior = priorAssistant.trim();
        return payload.length() < prior.length() && prior.startsWith(payload);
    }

    private static String cumulativeDelta(String alreadyEmitted, String incoming) {
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
