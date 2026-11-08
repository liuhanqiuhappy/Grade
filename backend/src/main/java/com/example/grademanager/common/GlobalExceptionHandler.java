package com.example.grademanager.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    /** 请求不是 multipart / 缺少文件字段：属参数问题，不能落到 5000。 */
    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Void> handleMultipart(MultipartException e) {
        return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(),
                "请以 form-data 方式上传文件，字段名为 file");
    }

    /** 缺少 multipart 文件字段（如导入时未带 file）：属于参数问题，不能落到 5000。 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ApiResponse<Void> handleMissingPart(MissingServletRequestPartException e) {
        return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(),
                "缺少上传文件字段：" + e.getRequestPartName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(),
                e.getParameterName() + ": 参数不能为空");
    }

    /** 路径/查询参数类型不匹配（如 /courses/abc/scores）。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(),
                e.getName() + ": 参数格式不正确");
    }

    /** 请求体不是合法 JSON / 字段类型不符。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadable(HttpMessageNotReadableException e) {
        return ApiResponse.fail(ErrorCode.INVALID_PARAM.getCode(), "请求体格式不正确，请检查 JSON 字段与类型");
    }

    /** 兜底：未知异常转 5000，记录完整堆栈但不暴露内部细节。 */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return ApiResponse.fail(ErrorCode.SYS_ERROR);
    }
}
