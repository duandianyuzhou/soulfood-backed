package com.food.soulfoodbackend.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录"),
    NOT_FOUND(404, "资源不存在"),
    FORBIDDEN(403, "无权访问"),
    CONFLICT(409, "操作冲突"),
    INTERNAL(500, "服务器错误");

    private final int code;
    private final String message;
}
