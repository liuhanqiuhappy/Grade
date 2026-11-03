package com.example.grademanager.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;

/**
 * 全局异常处理：业务失败同样返回 HTTP 200，用 code 区分（契约 §1.2）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    /** @Valid 校验请求体失败 → 4001，message 直接指出字段问题 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleInvalidBody(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        return ApiResponse.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(jakarta.validation.ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        return ApiResponse.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler({BindException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ApiResponse<Void> handleBadRequest(Exception ex) {
        return ApiResponse.fail(ErrorCode.PARAM_INVALID.getCode(), "请求参数格式不正确");
    }

    /**
     * 兜底：只捕获运行时异常，检查型异常（如 404 NoResourceFoundException）交给 Spring 默认处理，
     * 避免把「路径写错」伪装成业务成功。
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleUnexpected(RuntimeException ex) {
        log.error("未处理异常", ex);
        return ApiResponse.fail(ErrorCode.INTERNAL_ERROR);
    }
}
