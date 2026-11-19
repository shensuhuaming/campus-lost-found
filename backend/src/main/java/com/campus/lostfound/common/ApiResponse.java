package com.campus.lostfound.common;

/**
 * 统一响应体。
 * <p>
 * 约定（见《API接口详细规格》1.2/1.4）：code=200 表示成功，其余为业务错误码；
 * 分段：1xxx 参数与账号、2xxx 认证/权限/状态、3xxx 认领、4xxx 文件、5xxx 分类、9999 系统异常。
 *
 * @param code    业务状态码
 * @param message 提示信息
 * @param data    业务数据
 * @param <T>     数据类型
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
