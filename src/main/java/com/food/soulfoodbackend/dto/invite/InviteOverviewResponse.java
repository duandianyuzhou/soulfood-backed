package com.food.soulfoodbackend.dto.invite;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InviteOverviewResponse {

    private String inviteCode;
    private int completedCount;
    private int targetCount;
    private List<InviteRecordDto> records;
}
