package com.food.soulfoodbackend.dto.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateRoomRequest {

    @NotBlank
    @Size(max = 128)
    private String topic;

    @Min(2)
    @Max(20)
    private Integer maxPeople = 4;

    @Min(5)
    @Max(30)
    private Integer durationMin = 30;

    private List<@NotBlank String> initialOptions;
}
