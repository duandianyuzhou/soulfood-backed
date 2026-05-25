package com.food.soulfoodbackend.common;

import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusiness(BusinessException ex) {
        return ApiResult.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResult<Void> handleValidation(Exception ex) {
        return ApiResult.fail(ErrorCode.BAD_REQUEST.getCode(), ErrorCode.BAD_REQUEST.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleOther(Exception ex) {
        return ApiResult.fail(ErrorCode.INTERNAL.getCode(), ErrorCode.INTERNAL.getMessage());
    }
}
