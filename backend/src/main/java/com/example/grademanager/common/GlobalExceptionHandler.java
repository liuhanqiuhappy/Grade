package com.example.grademanager.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：HTTP 200 + 业务错误码。 */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败：统一转 4001，message 指出首个非法字段。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = ErrorCode.INVALID_PARAM.getMessage();
        if (e.getBindingResult().getFieldError() != null) {
            var fe = e.getBindingResult().getFieldError();
            message = fe.getField() + ": " + fe.getDefaultMessage();
        }
        return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(), message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleUpload(MaxUploadSizeExceededException e) {
        return ApiResponse.fail(ErrorCode.IMPORT_PARSE_FAIL.getCode(), "上传文件过大（限 5MB）");
    }

    /** 兜底：未知异常转 5000，记录完整堆栈但不暴露内部细节。 */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return ApiResponse.fail(ErrorCode.SYS_ERROR);
    }
}
