package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.friend.FriendChatConversationDto;
import com.food.soulfoodbackend.dto.friend.FriendChatMessageDto;
import com.food.soulfoodbackend.dto.friend.SendFriendMessageRequest;
import com.food.soulfoodbackend.service.FriendChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends/chat")
@RequiredArgsConstructor
public class FriendChatController {

    private final FriendChatService friendChatService;

    @GetMapping("/conversations")
    public ApiResult<List<FriendChatConversationDto>> listConversations() {
        return ApiResult.ok(friendChatService.listConversations(UserContext.requireUserId()));
    }

    @PostMapping("/conversations/{friendUserId}")
    public ApiResult<FriendChatConversationDto> openConversation(@PathVariable Long friendUserId) {
        return ApiResult.ok(friendChatService.openConversation(UserContext.requireUserId(), friendUserId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResult<List<FriendChatMessageDto>> listMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(friendChatService.listMessages(
                UserContext.requireUserId(), conversationId, beforeId, limit));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResult<FriendChatMessageDto> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendFriendMessageRequest request) {
        return ApiResult.ok(friendChatService.sendMessage(
                UserContext.requireUserId(), conversationId, request));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ApiResult<Void> markRead(@PathVariable Long conversationId) {
        friendChatService.markRead(UserContext.requireUserId(), conversationId);
        return ApiResult.ok(null);
    }
}
