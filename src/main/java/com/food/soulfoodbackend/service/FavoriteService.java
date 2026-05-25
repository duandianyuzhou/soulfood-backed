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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {

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
        if (!"recipe".equals(targetType) && !"restaurant".equals(targetType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "收藏类型无效");
        }

        LambdaQueryWrapper<SfFavorite> existingQuery = new LambdaQueryWrapper<SfFavorite>()
                .eq(SfFavorite::getUserId, userId)
                .eq(SfFavorite::getTargetType, targetType);
        if ("restaurant".equals(targetType) && request.getTargetId() != null) {
            existingQuery.eq(SfFavorite::getTargetId, request.getTargetId());
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

        SfFavorite row = new SfFavorite();
        row.setUserId(userId);
        row.setTargetType(targetType);
        row.setTargetId(request.getTargetId());
        row.setTitle(request.getTitle().trim());
        row.setSubtitle(request.getSubtitle());
        row.setMetaJson(meta.isEmpty() ? null : JsonStrings.toJson(meta));
        row.setCreatedAt(OffsetDateTime.now());
        row.setDeleted(false);
        favoriteMapper.insert(row);

        activityRecordService.recordFavorite(userId, request.getTitle());
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
        return new FavoriteItemDto(
                row.getId(),
                row.getTargetType(),
                row.getTargetId(),
                row.getTitle(),
                row.getSubtitle(),
                meta.get("category"),
                meta.get("score"),
                meta.get("duration"));
    }
}
