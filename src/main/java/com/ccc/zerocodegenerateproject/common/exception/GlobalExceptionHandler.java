package com.ccc.zerocodegenerateproject.common.exception;

import com.ccc.zerocodegenerateproject.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 使用 @RestControllerAdvice 自动返回 JSON 格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理自定义业务异常（最常用）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e) {
        log.error("业务异常：code={}, msg={}", e.getCode(), e.getMsg(), e);
        return Result.error(e.getCode(), e.getMsg());
    }

    /**
     * 处理参数校验异常（@Valid + @RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleValidException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数校验失败: {}", errorMsg);
        return Result.error(400, "参数校验失败: " + errorMsg);
    }

    /**
     * 处理普通参数绑定异常（@RequestParam 等）
     */
    @ExceptionHandler(BindException.class)
    public Result<Object> handleBindException(BindException e) {
        String errorMsg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数绑定失败: {}", errorMsg);
        return Result.error(400, "参数错误: " + errorMsg);
    }

    /**
     * 处理空指针等运行时异常（兜底处理）
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Object> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Result.error(500, "服务器内部错误：空指针异常");
    }

    /**
     * 兜底异常处理（所有未捕获的异常）
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        log.error("系统未知异常", e);
        return Result.error(500, "服务器繁忙，请稍后重试");
    }
}