package com.food.soulfoodbackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "请输入用户名")
    private String username;

    @NotBlank(message = "请输入绑定的手机号")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入 11 位手机号")
    private String phone;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, message = "新密码至少 6 位")
    private String newPassword;
}
