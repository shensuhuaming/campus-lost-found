package com.campus.lostfound.common;

/**
 * 统一响应体。
 * <p>
 * 约定（待接口契约评审确认）：code=0 表示成功，非 0 表示业务错误；
 * 错误码分段建议：1xxxx 通用/参数、2xxxx 用户、3xxxx 物品、4xxxx 认领、5xxxx 留言消息、9xxxx 系统。
 *
 * @param code    业务状态码
 * @param message 提示信息
 * @param data    业务数据
 * @param <T>     数据类型
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
