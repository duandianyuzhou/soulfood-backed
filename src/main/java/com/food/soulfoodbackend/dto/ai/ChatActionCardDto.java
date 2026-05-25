package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatActionCardDto {

    /** recipe | restaurant */
    private String type;
    private Long id;
    private String name;
    private String subtitle;
}
