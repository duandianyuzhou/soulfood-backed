package com.food.soulfoodbackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class JsonStrings {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonStrings() {
    }

    static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value == null ? Collections.emptyList() : value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    static List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    static Map<String, String> parseStringMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
