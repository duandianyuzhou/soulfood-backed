package com.food.soulfoodbackend.dto.friend;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddFriendRequest {

    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;
}
