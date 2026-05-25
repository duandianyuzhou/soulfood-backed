package com.food.soulfoodbackend.dto.record;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecordsOverviewResponse {

    private List<RecordStatDto> stats;
    private List<RecordItemDto> recent;
}
