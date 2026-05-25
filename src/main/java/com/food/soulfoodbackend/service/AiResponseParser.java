package com.food.soulfoodbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.dto.ai.RecipeItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AiResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");

    private AiResponseParser() {
    }

    static List<RecipeItemDto> parseRecipes(String text) {
        List<RecipeItemDto> fromJson = tryParseRecipeJson(text);
        if (!fromJson.isEmpty()) {
            return fromJson;
        }
        List<RecipeItemDto> items = new ArrayList<>();
        for (String line : text.split("\n")) {
            String cleaned = line.replaceAll("^[\\d\\s\\.、\\)（(-•*]+", "").trim();
            if (cleaned.length() < 2) {
                continue;
            }
            String name = cleaned;
            String reason = "";
            int colon = cleaned.indexOf('：');
            if (colon < 0) {
                colon = cleaned.indexOf(':');
            }
            if (colon > 0) {
                name = cleaned.substring(0, colon).trim();
                reason = cleaned.substring(colon + 1).trim();
            }
            Matcher scoreMatcher = Pattern.compile("(\\d\\.\\d)").matcher(line);
            double score = scoreMatcher.find() ? Double.parseDouble(scoreMatcher.group(1)) : 4.5;
            items.add(new RecipeItemDto(name, score, reason.isBlank() ? "AI 推荐" : reason));
            if (items.size() >= 5) {
                break;
            }
        }
        return items;
    }

    private static List<RecipeItemDto> tryParseRecipeJson(String text) {
        try {
            Matcher matcher = JSON_BLOCK.matcher(text);
            if (!matcher.find()) {
                return List.of();
            }
            JsonNode root = MAPPER.readTree(matcher.group());
            JsonNode items = root.get("items");
            if (items == null || !items.isArray()) {
                return List.of();
            }
            List<RecipeItemDto> result = new ArrayList<>();
            for (JsonNode node : items) {
                String name = node.path("name").asText("").trim();
                if (name.isBlank()) {
                    continue;
                }
                double score = node.path("score").asDouble(4.5);
                String reason = node.path("reason").asText("AI 推荐");
                result.add(new RecipeItemDto(name, score, reason));
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    static List<String> parseOptions(String text) {
        List<String> options = new ArrayList<>();
        for (String line : text.split("\n")) {
            String cleaned = line.replaceAll("^[\\d\\s\\.、\\)（(-•*\\[\\]\"]+", "").trim();
            if (cleaned.length() >= 2 && cleaned.length() <= 16) {
                options.add(cleaned);
            }
            if (options.size() >= 4) {
                break;
            }
        }
        return options;
    }

    static RandomPickResult parsePick(String text, List<String> candidates) {
        try {
            Matcher matcher = JSON_BLOCK.matcher(text);
            if (matcher.find()) {
                JsonNode root = MAPPER.readTree(matcher.group());
                String name = root.path("name").asText("").trim();
                String reason = root.path("reason").asText("").trim();
                if (!name.isBlank()) {
                    return new RandomPickResult(matchCandidate(name, candidates), reason, text);
                }
            }
        } catch (Exception ignored) {
            // fallback below
        }
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                String reason = text.lines().findFirst().orElse("AI 推荐");
                return new RandomPickResult(candidate, reason, text);
            }
        }
        String first = candidates.isEmpty() ? "推荐餐厅" : candidates.get(0);
        return new RandomPickResult(first, text.lines().findFirst().orElse("AI 推荐"), text);
    }

    private static String matchCandidate(String name, List<String> candidates) {
        for (String c : candidates) {
            if (name.contains(c) || c.contains(name)) {
                return c;
            }
        }
        return name;
    }

    record RandomPickResult(String name, String reason, String rawReply) {
    }
}
