package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.ai.AddAiMemoryRequest;
import com.food.soulfoodbackend.dto.ai.AiUserMemoryDto;
import com.food.soulfoodbackend.service.AiUserMemoryService;
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
@RequestMapping("/ai/memory")
@RequiredArgsConstructor
public class AiMemoryController {

    private final AiUserMemoryService memoryService;

    @GetMapping
    public ApiResult<List<AiUserMemoryDto>> list(
            @RequestParam(required = false) String memoryType) {
        return ApiResult.ok(memoryService.listForUser(UserContext.getUserId(), memoryType));
    }

    @PostMapping
    public ApiResult<AiUserMemoryDto> add(@Valid @RequestBody AddAiMemoryRequest request) {
        return ApiResult.ok(memoryService.addUserMemory(
                UserContext.getUserId(),
                request.getContent(),
                request.getMemoryType()));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Map<String, String>> delete(@PathVariable Long id) {
        memoryService.deleteMemory(UserContext.getUserId(), id);
        return ApiResult.ok(Map.of("status", "deleted"));
    }
}
