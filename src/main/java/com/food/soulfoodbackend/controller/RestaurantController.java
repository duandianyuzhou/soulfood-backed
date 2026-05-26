package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.restaurant.RestaurantDto;
import com.food.soulfoodbackend.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/nearby")
    public ApiResult<List<RestaurantDto>> nearby(
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(restaurantService.listNearby(UserContext.getUserId(), lng, lat, category, keyword));
    }

    @GetMapping("/wanted")
    public ApiResult<List<RestaurantDto>> wanted() {
        return ApiResult.ok(restaurantService.listWanted(UserContext.requireUserId()));
    }

    @GetMapping("/{id}")
    public ApiResult<RestaurantDto> detail(@PathVariable Long id) {
        return ApiResult.ok(restaurantService.getById(UserContext.getUserId(), id));
    }

    @PostMapping("/{id}/want")
    public ApiResult<Boolean> want(@PathVariable Long id) {
        restaurantService.markWant(UserContext.requireUserId(), id);
        return ApiResult.ok(Boolean.TRUE);
    }

    @DeleteMapping("/{id}/want")
    public ApiResult<Void> removeWant(@PathVariable Long id) {
        restaurantService.removeWant(UserContext.requireUserId(), id);
        return ApiResult.ok(null);
    }
}
