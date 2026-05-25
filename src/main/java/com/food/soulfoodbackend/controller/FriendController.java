package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.friend.AddFriendRequest;
import com.food.soulfoodbackend.dto.friend.FriendItemDto;
import com.food.soulfoodbackend.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public ApiResult<List<FriendItemDto>> list() {
        return ApiResult.ok(friendService.listFriends(UserContext.requireUserId()));
    }

    @PostMapping
    public ApiResult<FriendItemDto> add(@Valid @RequestBody AddFriendRequest request) {
        return ApiResult.ok(friendService.addFriendByInviteCode(UserContext.requireUserId(), request));
    }
}
