package com.food.soulfoodbackend.dto.record;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecordStatDto {

    private String key;
    private String title;
    private int count;
}
