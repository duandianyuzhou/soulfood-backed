package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.room.AddOptionRequest;
import com.food.soulfoodbackend.dto.room.CastVoteRequest;
import com.food.soulfoodbackend.dto.room.CreateRoomRequest;
import com.food.soulfoodbackend.dto.room.CreateRoomResponse;
import com.food.soulfoodbackend.dto.room.FriendRoomDto;
import com.food.soulfoodbackend.dto.room.RoomDetailResponse;
import com.food.soulfoodbackend.dto.room.RoomOptionDto;
import com.food.soulfoodbackend.dto.room.RoomResultResponse;
import com.food.soulfoodbackend.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ApiResult<CreateRoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        return ApiResult.ok(roomService.createRoom(UserContext.requireUserId(), request));
    }

    @GetMapping("/friends")
    public ApiResult<List<FriendRoomDto>> friendRooms() {
        return ApiResult.ok(roomService.listFriendRooms(UserContext.requireUserId()));
    }

    @GetMapping("/{code}")
    public ApiResult<RoomDetailResponse> detail(@PathVariable String code) {
        return ApiResult.ok(roomService.getRoomDetail(code, UserContext.getUserId()));
    }

    @PostMapping("/{code}/join")
    public ApiResult<RoomDetailResponse> join(@PathVariable String code) {
        return ApiResult.ok(roomService.joinRoom(code, UserContext.requireUserId()));
    }

    @PostMapping("/{code}/options")
    public ApiResult<RoomOptionDto> addOption(@PathVariable String code, @Valid @RequestBody AddOptionRequest request) {
        return ApiResult.ok(roomService.addOption(code, request));
    }

    @PostMapping("/{code}/votes")
    public ApiResult<RoomDetailResponse> vote(@PathVariable String code, @Valid @RequestBody CastVoteRequest request) {
        return ApiResult.ok(roomService.castVote(code, UserContext.requireUserId(), request));
    }

    @GetMapping("/{code}/result")
    public ApiResult<RoomResultResponse> result(@PathVariable String code) {
        return ApiResult.ok(roomService.getResult(code, UserContext.getUserId()));
    }
}
