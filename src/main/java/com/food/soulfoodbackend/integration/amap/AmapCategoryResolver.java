package com.food.soulfoodbackend.integration.amap;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AmapCategoryResolver {

    public String resolveKeywords(String category, String keyword) {
        if (StringUtils.hasText(keyword)) {
            return keyword.trim();
        }
        if (!StringUtils.hasText(category) || "all".equalsIgnoreCase(category)) {
            return "美食|餐厅";
        }
        return switch (category.toLowerCase()) {
            case "hotpot" -> "火锅";
            case "drink" -> "奶茶|饮品|咖啡";
            case "fast" -> "快餐|麦当劳|肯德基";
            case "bbq" -> "烤肉|烧烤";
            default -> "美食|餐厅";
        };
    }

    public String resolveTypes() {
        return "050000";
    }

    public String labelFromType(String typeField) {
        if (!StringUtils.hasText(typeField)) {
            return "餐饮";
        }
        String[] parts = typeField.split(";");
        if (parts.length == 0) {
            return "餐饮";
        }
        String last = parts[parts.length - 1].trim();
        return last.isEmpty() ? "餐饮" : last;
    }
}
