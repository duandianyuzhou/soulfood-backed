package com.food.soulfoodbackend.dto.record;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecordItemDto {

    private Long id;
    private String recordType;
    private String title;
    private String detail;
    private String occurredAt;
}
