package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.domain.entity.SfUserPreference;
import com.food.soulfoodbackend.dto.preference.PreferenceResponse;
import com.food.soulfoodbackend.dto.preference.UpdatePreferenceRequest;
import com.food.soulfoodbackend.mapper.SfUserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private static final List<String> DEFAULT_TAGS = List.of("清淡", "家常菜");

    private final SfUserPreferenceMapper preferenceMapper;

    public PreferenceResponse getPreference(Long userId) {
        SfUserPreference row = findOrCreate(userId);
        return toResponse(row);
    }

    @Transactional
    public PreferenceResponse updatePreference(Long userId, UpdatePreferenceRequest request) {
        SfUserPreference row = findOrCreate(userId);
        row.setTagsJson(JsonStrings.toJson(request.getTags() == null ? List.of() : request.getTags()));
        row.setSpicyLevel(request.getSpicyLevel());
        row.setSweetLevel(request.getSweetLevel());
        row.setNoCoriander(Boolean.TRUE.equals(request.getNoCoriander()));
        row.setNoPeanut(Boolean.TRUE.equals(request.getNoPeanut()));
        row.setAllergensJson(JsonStrings.toJson(request.getAllergens() == null ? List.of() : request.getAllergens()));
        row.setUpdatedAt(OffsetDateTime.now());
        preferenceMapper.updateById(row);
        return toResponse(row);
    }

    public String buildPreferenceText(Long userId) {
        return toResponse(findOrCreate(userId)).getPreferenceText();
    }

    private SfUserPreference findOrCreate(Long userId) {
        SfUserPreference row = preferenceMapper.selectOne(
                new LambdaQueryWrapper<SfUserPreference>().eq(SfUserPreference::getUserId, userId));
        if (row != null) {
            return row;
        }
        row = new SfUserPreference();
        row.setUserId(userId);
        row.setTagsJson(JsonStrings.toJson(DEFAULT_TAGS));
        row.setSpicyLevel(3);
        row.setSweetLevel(2);
        row.setNoCoriander(false);
        row.setNoPeanut(false);
        row.setAllergensJson("[]");
        row.setCreatedAt(OffsetDateTime.now());
        row.setUpdatedAt(OffsetDateTime.now());
        preferenceMapper.insert(row);
        return row;
    }

    private PreferenceResponse toResponse(SfUserPreference row) {
        List<String> tags = JsonStrings.parseStringList(row.getTagsJson());
        if (tags.isEmpty()) {
            tags = DEFAULT_TAGS;
        }
        List<String> allergens = JsonStrings.parseStringList(row.getAllergensJson());
        return new PreferenceResponse(
                tags,
                row.getSpicyLevel() == null ? 3 : row.getSpicyLevel(),
                row.getSweetLevel() == null ? 2 : row.getSweetLevel(),
                Boolean.TRUE.equals(row.getNoCoriander()),
                Boolean.TRUE.equals(row.getNoPeanut()),
                allergens,
                buildText(tags, row, allergens));
    }

    private String buildText(List<String> tags, SfUserPreference row, List<String> allergens) {
        List<String> parts = new ArrayList<>(tags);
        parts.add("辣度" + (row.getSpicyLevel() == null ? 3 : row.getSpicyLevel()) + "/5");
        parts.add("甜度" + (row.getSweetLevel() == null ? 2 : row.getSweetLevel()) + "/5");
        if (Boolean.TRUE.equals(row.getNoCoriander())) {
            parts.add("不吃香菜");
        }
        if (Boolean.TRUE.equals(row.getNoPeanut())) {
            parts.add("花生过敏");
        }
        if (!allergens.isEmpty()) {
            parts.add("忌口：" + String.join("、", allergens));
        }
        return parts.stream().filter(s -> s != null && !s.isBlank()).collect(Collectors.joining("、"));
    }
}
