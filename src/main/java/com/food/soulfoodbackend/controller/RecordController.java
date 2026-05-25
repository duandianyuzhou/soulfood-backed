package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.record.RecordsOverviewResponse;
import com.food.soulfoodbackend.service.ActivityRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {

    private final ActivityRecordService activityRecordService;

    @GetMapping("/overview")
    public ApiResult<RecordsOverviewResponse> overview() {
        return ApiResult.ok(activityRecordService.overview(UserContext.requireUserId()));
    }
}
