package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.config.AmapProperties;
import com.food.soulfoodbackend.domain.entity.SfFavorite;
import com.food.soulfoodbackend.domain.entity.SfRestaurant;
import com.food.soulfoodbackend.domain.entity.SfRestaurantWant;
import com.food.soulfoodbackend.dto.restaurant.RestaurantDto;
import com.food.soulfoodbackend.integration.amap.AmapPoi;
import com.food.soulfoodbackend.integration.amap.AmapPoiClient;
import com.food.soulfoodbackend.mapper.SfFavoriteMapper;
import com.food.soulfoodbackend.mapper.SfRestaurantMapper;
import com.food.soulfoodbackend.mapper.SfRestaurantWantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final SfRestaurantMapper restaurantMapper;
    private final SfRestaurantWantMapper restaurantWantMapper;
    private final SfFavoriteMapper favoriteMapper;
    private final ActivityRecordService activityRecordService;
    private final AmapPoiClient amapPoiClient;
    private final AmapProperties amapProperties;

    @Transactional
    public List<RestaurantDto> listNearby(Long userId, Double lng, Double lat, String category, String keyword) {
        double queryLng = lng != null ? lng : amapProperties.getDefaultLng();
        double queryLat = lat != null ? lat : amapProperties.getDefaultLat();

        List<AmapPoi> pois = amapPoiClient.searchNearby(queryLng, queryLat, category, keyword);
        Set<Long> wantedIds = loadWantedIds(userId);
        Set<Long> favoritedIds = loadFavoritedIds(userId);

        return pois.stream()
                .map(poi -> upsertFromAmap(poi))
                .sorted(Comparator.comparing(
                        SfRestaurant::getDistanceKm,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(row -> toDto(row, wantedIds.contains(row.getId()), favoritedIds.contains(row.getId())))
                .toList();
    }

    public RestaurantDto getById(Long userId, Long restaurantId) {
        SfRestaurant restaurant = restaurantMapper.selectById(restaurantId);
        if (restaurant == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "餐厅不存在");
        }
        boolean wanted = false;
        boolean favorited = false;
        if (userId != null) {
            wanted = restaurantWantMapper.selectCount(new LambdaQueryWrapper<SfRestaurantWant>()
                    .eq(SfRestaurantWant::getUserId, userId)
                    .eq(SfRestaurantWant::getRestaurantId, restaurantId)) > 0;
            favorited = favoriteMapper.selectCount(new LambdaQueryWrapper<SfFavorite>()
                    .eq(SfFavorite::getUserId, userId)
                    .eq(SfFavorite::getTargetType, "restaurant")
                    .eq(SfFavorite::getTargetId, restaurantId)) > 0;
        }
        return toDto(restaurant, wanted, favorited);
    }

    public void markWant(Long userId, Long restaurantId) {
        SfRestaurant restaurant = restaurantMapper.selectById(restaurantId);
        if (restaurant == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "餐厅不存在");
        }
        Long count = restaurantWantMapper.selectCount(new LambdaQueryWrapper<SfRestaurantWant>()
                .eq(SfRestaurantWant::getUserId, userId)
                .eq(SfRestaurantWant::getRestaurantId, restaurantId));
        if (count > 0) {
            return;
        }
        SfRestaurantWant want = new SfRestaurantWant();
        want.setUserId(userId);
        want.setRestaurantId(restaurantId);
        want.setCreatedAt(OffsetDateTime.now());
        restaurantWantMapper.insert(want);
        activityRecordService.recordWantRestaurant(userId, restaurant.getName());
    }

    public List<RestaurantDto> listWanted(Long userId) {
        List<SfRestaurantWant> wants = restaurantWantMapper.selectList(new LambdaQueryWrapper<SfRestaurantWant>()
                .eq(SfRestaurantWant::getUserId, userId)
                .orderByDesc(SfRestaurantWant::getCreatedAt));
        if (wants.isEmpty()) {
            return List.of();
        }
        Set<Long> favoritedIds = loadFavoritedIds(userId);
        List<RestaurantDto> result = new ArrayList<>();
        for (SfRestaurantWant want : wants) {
            SfRestaurant restaurant = restaurantMapper.selectById(want.getRestaurantId());
            if (restaurant != null) {
                result.add(toDto(restaurant, true, favoritedIds.contains(restaurant.getId())));
            }
        }
        return result;
    }

    @Transactional
    public void removeWant(Long userId, Long restaurantId) {
        restaurantWantMapper.delete(new LambdaQueryWrapper<SfRestaurantWant>()
                .eq(SfRestaurantWant::getUserId, userId)
                .eq(SfRestaurantWant::getRestaurantId, restaurantId));
    }

    private SfRestaurant upsertFromAmap(AmapPoi poi) {
        SfRestaurant row = restaurantMapper.selectOne(new LambdaQueryWrapper<SfRestaurant>()
                .eq(SfRestaurant::getExternalId, poi.id())
                .last("LIMIT 1"));
        OffsetDateTime now = OffsetDateTime.now();
        if (row == null) {
            row = new SfRestaurant();
            row.setExternalId(poi.id());
            row.setCreatedAt(now);
            row.setDeleted(false);
        }
        row.setName(poi.name());
        row.setCategory(poi.categoryLabel());
        row.setAddress(poi.address());
        row.setLng(poi.lng());
        row.setLat(poi.lat());
        row.setDistanceKm(poi.distanceKm());
        row.setRating(poi.rating());
        row.setPhone(poi.phone());
        row.setPhotoUrl(poi.photoUrl());
        row.setUpdatedAt(now);
        if (row.getId() == null) {
            restaurantMapper.insert(row);
        } else {
            restaurantMapper.updateById(row);
        }
        return row;
    }

    private Set<Long> loadWantedIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return restaurantWantMapper.selectList(new LambdaQueryWrapper<SfRestaurantWant>()
                        .eq(SfRestaurantWant::getUserId, userId))
                .stream()
                .map(SfRestaurantWant::getRestaurantId)
                .collect(Collectors.toSet());
    }

    private Set<Long> loadFavoritedIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        return favoriteMapper.selectList(new LambdaQueryWrapper<SfFavorite>()
                        .eq(SfFavorite::getUserId, userId)
                        .eq(SfFavorite::getTargetType, "restaurant")
                        .isNotNull(SfFavorite::getTargetId))
                .stream()
                .map(SfFavorite::getTargetId)
                .collect(Collectors.toSet());
    }

    private RestaurantDto toDto(SfRestaurant restaurant, boolean wanted, boolean favorited) {
        return new RestaurantDto(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCategory(),
                restaurant.getRating(),
                restaurant.getDistanceKm(),
                restaurant.getAddress(),
                restaurant.getLat(),
                restaurant.getLng(),
                restaurant.getPhone(),
                restaurant.getPhotoUrl(),
                wanted,
                favorited);
    }
}
