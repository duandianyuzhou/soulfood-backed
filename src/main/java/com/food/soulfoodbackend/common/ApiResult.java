package com.food.soulfoodbackend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;
    private String traceId;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data, null);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null, null);
    }
}
