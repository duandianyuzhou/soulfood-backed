package com.food.soulfoodbackend.ai.rag;

import org.springframework.util.StringUtils;

public final class KnowledgeQueryDetector {

    private KnowledgeQueryDetector() {
    }

    public static boolean looksLikeKnowledgeQuery(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String text = message.trim();
        return containsAny(text,
                "怎么做", "做法", "步骤", "食谱", "怎么煮", "怎么炒", "怎么炖",
                "隔夜", "房间号", "加入房间", "怎么加入", "怎么用", "忌口",
                "土豆丝", "红烧肉", "番茄炒蛋", "麻婆", "冷藏", "油温",
                "DecideMeal", "收藏能", "需要登录", "想去");
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
