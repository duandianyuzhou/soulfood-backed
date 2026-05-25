package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfFavorite;
import com.food.soulfoodbackend.dto.favorite.AddFavoriteRequest;
import com.food.soulfoodbackend.dto.favorite.FavoriteItemDto;
import com.food.soulfoodbackend.mapper.SfFavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private static final int SUBTITLE_MAX = 256;

    private final SfFavoriteMapper favoriteMapper;
    private final ActivityRecordService activityRecordService;

    public List<FavoriteItemDto> listFavorites(Long userId, String type) {
        LambdaQueryWrapper<SfFavorite> wrapper = new LambdaQueryWrapper<SfFavorite>()
                .eq(SfFavorite::getUserId, userId)
                .orderByDesc(SfFavorite::getCreatedAt);
        if (type != null && !type.isBlank() && !"all".equalsIgnoreCase(type)) {
            wrapper.eq(SfFavorite::getTargetType, type);
        }
        return favoriteMapper.selectList(wrapper).stream().map(this::toDto).toList();
    }

    @Transactional
    public FavoriteItemDto addFavorite(Long userId, AddFavoriteRequest request) {
        String targetType = request.getTargetType().trim();
        if (!"recipe".equals(targetType) && !"restaurant".equals(targetType) && !"ai_reply".equals(targetType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "收藏类型无效");
        }

        LambdaQueryWrapper<SfFavorite> existingQuery = new LambdaQueryWrapper<SfFavorite>()
                .eq(SfFavorite::getUserId, userId)
                .eq(SfFavorite::getTargetType, targetType);
        if (request.getTargetId() != null && ("restaurant".equals(targetType) || "recipe".equals(targetType))) {
            existingQuery.eq(SfFavorite::getTargetId, request.getTargetId());
        } else if ("ai_reply".equals(targetType) && StringUtils.hasText(request.getContent())) {
            existingQuery.eq(SfFavorite::getTitle, buildAiReplyTitle(request.getContent()));
        } else {
            existingQuery.eq(SfFavorite::getTitle, request.getTitle().trim());
        }
        SfFavorite existing = favoriteMapper.selectOne(existingQuery.last("LIMIT 1"));
        if (existing != null) {
            return toDto(existing);
        }

        Map<String, String> meta = new HashMap<>();
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            meta.put("category", request.getCategory());
        }
        if (request.getScore() != null && !request.getScore().isBlank()) {
            meta.put("score", request.getScore());
        }
        if (request.getDuration() != null && !request.getDuration().isBlank()) {
            meta.put("duration", request.getDuration());
        }
        if ("ai_reply".equals(targetType) && StringUtils.hasText(request.getContent())) {
            meta.put("fullContent", request.getContent());
        }
        if (StringUtils.hasText(request.getConversationId())) {
            meta.put("conversationId", request.getConversationId().trim());
        }

        String title = request.getTitle().trim();
        String subtitle = request.getSubtitle();
        if ("ai_reply".equals(targetType) && StringUtils.hasText(request.getContent())) {
            title = buildAiReplyTitle(request.getContent());
            subtitle = truncate(request.getContent(), SUBTITLE_MAX);
        }

        SfFavorite row = new SfFavorite();
        row.setUserId(userId);
        row.setTargetType(targetType);
        row.setTargetId(request.getTargetId());
        row.setTitle(title);
        row.setSubtitle(subtitle);
        row.setMetaJson(meta.isEmpty() ? null : JsonStrings.toJson(meta));
        row.setCreatedAt(OffsetDateTime.now());
        row.setDeleted(false);
        favoriteMapper.insert(row);

        activityRecordService.recordFavorite(userId, title);
        return toDto(row);
    }

    @Transactional
    public void removeFavorite(Long userId, Long favoriteId) {
        SfFavorite row = favoriteMapper.selectById(favoriteId);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收藏不存在");
        }
        favoriteMapper.deleteById(favoriteId);
    }

    @Transactional
    public void removeByTarget(Long userId, String targetType, Long targetId) {
        SfFavorite row = favoriteMapper.selectOne(new LambdaQueryWrapper<SfFavorite>()
                .eq(SfFavorite::getUserId, userId)
                .eq(SfFavorite::getTargetType, targetType)
                .eq(SfFavorite::getTargetId, targetId)
                .last("LIMIT 1"));
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收藏不存在");
        }
        favoriteMapper.deleteById(row.getId());
    }

    private FavoriteItemDto toDto(SfFavorite row) {
        Map<String, String> meta = JsonStrings.parseStringMap(row.getMetaJson());
        String content = meta.get("fullContent");
        if (content == null && "ai_reply".equals(row.getTargetType()) && row.getSubtitle() != null) {
            content = row.getSubtitle();
        }
        return new FavoriteItemDto(
                row.getId(),
                row.getTargetType(),
                row.getTargetId(),
                row.getTitle(),
                row.getSubtitle(),
                meta.get("category"),
                meta.get("score"),
                meta.get("duration"),
                content,
                meta.get("conversationId"));
    }

    private static String buildAiReplyTitle(String content) {
        String line = content.lines()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse("AI 回复");
        return truncate(line.replaceAll("[#*`>]", "").trim(), 64);
    }

    private static String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }
}
