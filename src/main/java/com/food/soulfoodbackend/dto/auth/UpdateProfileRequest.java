package com.food.soulfoodbackend.dto.auth;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 32, message = "昵称最多 32 字")
    private String nickname;

    @Size(max = 512, message = "头像链接过长")
    private String avatarUrl;

    @Size(max = 120, message = "签名最多 120 字")
    private String signature;
}
