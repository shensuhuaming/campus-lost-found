package com.campus.lostfound.common;

/**
 * 业务异常：携带 {@link ResultCode}，由全局异常处理器转换为统一响应。
 * 属于可预期异常，不打堆栈。
 */
public class BusinessException extends RuntimeException {

    private final ResultCode code;

    public BusinessException(ResultCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public BusinessException(ResultCode code, String message) {
        super(message);
        this.code = code;
    }

    public ResultCode getCode() {
        return code;
    }
}
