package com.sunxu.appforge.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code; // 状态码

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
