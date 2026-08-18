package com.food.soulfoodbackend.ai.rag;

import org.springframework.util.StringUtils;

public final class ReactNeedDetector {

    private ReactNeedDetector() {
    }

    public static boolean needsReact(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        return containsAny(message.trim(),
                "附近", "探店", "外卖", "地图",
                "投票", "组局", "开房", "创建房间", "建个房",
                "收藏这", "帮我收藏", "加入收藏",
                "搜餐厅", "找餐厅", "找家店", "推荐几家", "有什么店",
                "火锅店", "烧烤店", "日料店");
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
