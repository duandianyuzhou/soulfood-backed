package com.food.soulfoodbackend.dto.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendFriendMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;
}
