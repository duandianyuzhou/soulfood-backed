package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.favorite.AddFavoriteRequest;
import com.food.soulfoodbackend.dto.favorite.FavoriteItemDto;
import com.food.soulfoodbackend.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ApiResult<List<FavoriteItemDto>> list(@RequestParam(required = false) String type) {
        return ApiResult.ok(favoriteService.listFavorites(UserContext.requireUserId(), type));
    }

    @PostMapping
    public ApiResult<FavoriteItemDto> add(@Valid @RequestBody AddFavoriteRequest request) {
        return ApiResult.ok(favoriteService.addFavorite(UserContext.requireUserId(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Map<String, String>> remove(@PathVariable Long id) {
        favoriteService.removeFavorite(UserContext.requireUserId(), id);
        return ApiResult.ok(Map.of("status", "removed"));
    }
}
