package com.example.grademanager.common;

import lombok.Getter;

/** 业务异常：携带错误码，由全局异常处理器统一包装为 HTTP 200 响应。 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
