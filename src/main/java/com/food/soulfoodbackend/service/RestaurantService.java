package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfRestaurant;
import com.food.soulfoodbackend.domain.entity.SfRestaurantWant;
import com.food.soulfoodbackend.dto.restaurant.RestaurantDto;
import com.food.soulfoodbackend.mapper.SfRestaurantMapper;
import com.food.soulfoodbackend.mapper.SfRestaurantWantMapper;
import com.food.soulfoodbackend.service.ActivityRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final SfRestaurantMapper restaurantMapper;
    private final SfRestaurantWantMapper restaurantWantMapper;
    private final ActivityRecordService activityRecordService;

    public List<RestaurantDto> listNearby(Long userId, String category, String keyword) {
        LambdaQueryWrapper<SfRestaurant> wrapper = new LambdaQueryWrapper<SfRestaurant>()
                .orderByAsc(SfRestaurant::getDistanceKm);
        if (StringUtils.hasText(category) && !"all".equalsIgnoreCase(category)) {
            wrapper.eq(SfRestaurant::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SfRestaurant::getName, keyword).or().like(SfRestaurant::getCategory, keyword));
        }
        Set<Long> wantedIds = Set.of();
        if (userId != null) {
            wantedIds = restaurantWantMapper.selectList(new LambdaQueryWrapper<SfRestaurantWant>()
                            .eq(SfRestaurantWant::getUserId, userId))
                    .stream()
                    .map(SfRestaurantWant::getRestaurantId)
                    .collect(Collectors.toSet());
        }
        Set<Long> finalWantedIds = wantedIds;
        return restaurantMapper.selectList(wrapper).stream()
                .map(r -> new RestaurantDto(
                        r.getId(),
                        r.getName(),
                        r.getCategory(),
                        r.getRating(),
                        r.getDistanceKm(),
                        r.getAddress(),
                        finalWantedIds.contains(r.getId())))
                .toList();
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
}
