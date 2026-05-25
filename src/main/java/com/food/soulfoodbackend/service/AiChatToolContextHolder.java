package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public final class AiChatToolContextHolder {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private AiChatToolContextHolder() {
    }

    public static void set(Long userId, Double lat, Double lng, String conversationId) {
        Context ctx = new Context();
        ctx.userId = userId;
        ctx.lat = lat;
        ctx.lng = lng;
        ctx.conversationId = conversationId;
        HOLDER.set(ctx);
    }

    public static Context get() {
        return HOLDER.get();
    }

    public static Long userId() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.userId : null;
    }

    public static Double lat() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.lat : null;
    }

    public static Double lng() {
        Context ctx = HOLDER.get();
        return ctx != null ? ctx.lng : null;
    }

    public static void addCard(ChatActionCardDto card) {
        Context ctx = HOLDER.get();
        if (ctx != null && card != null) {
            ctx.toolCards.add(card);
        }
    }

    public static List<ChatActionCardDto> toolCards() {
        Context ctx = HOLDER.get();
        return ctx != null ? List.copyOf(ctx.toolCards) : List.of();
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Getter
    @Setter
    public static final class Context {
        private Long userId;
        private Double lat;
        private Double lng;
        private String conversationId;
        private final List<ChatActionCardDto> toolCards = new ArrayList<>();
    }
}
