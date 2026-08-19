package com.food.soulfoodbackend.ai.router;

import com.food.soulfoodbackend.ai.rag.KnowledgeQueryDetector;
import com.food.soulfoodbackend.ai.rag.ReactNeedDetector;
import org.springframework.util.StringUtils;

public final class RuleIntentRouter {

    private RuleIntentRouter() {
    }

    /**
     * @return 确定意图；不够确定时返回 null，交给 LLM
     */
    public static ChatIntent tryRoute(String message, boolean hasImage, boolean hasLocation) {
        if (hasImage) {
            return ChatIntent.COOK_FROM_FRIDGE;
        }
        if (!StringUtils.hasText(message)) {
            return ChatIntent.SIMPLE_CHAT;
        }
        String text = message.trim();
        if (KnowledgeQueryDetector.looksLikeKnowledgeQuery(text)
                && !ReactNeedDetector.needsNearbySearch(text)) {
            return ChatIntent.RECIPE_RAG;
        }
        if (isVote(text)) {
            return ChatIntent.VOTE_ROOM;
        }
        if (ReactNeedDetector.needsNearbySearch(text)) {
            return ChatIntent.NEARBY_EAT;
        }
        if (isTonightEat(text) && hasLocation) {
            return ChatIntent.NEARBY_EAT;
        }
        if (ReactNeedDetector.needsReact(text)) {
            return ChatIntent.OPEN_REACT;
        }
        if (isAmbiguousEat(text)) {
            return null;
        }
        return ChatIntent.SIMPLE_CHAT;
    }

    static boolean isVote(String text) {
        return containsAny(text, "投票", "组局", "开房", "创建房间", "建个房", "一起吃");
    }

    static boolean isTonightEat(String text) {
        return containsAny(text, "今晚吃什么", "晚上吃什么", "中午吃什么", "吃点啥", "吃什么好");
    }

    static boolean isAmbiguousEat(String text) {
        return isTonightEat(text) || containsAny(text, "随便吃", "不知道吃什么", "吃啥啊", "决定不了");
    }

    private static boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
