package com.campus.lostfound.common;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码（对齐《API接口详细规格》1.4 错误码总表）。
 * <p>
 * 分段：1xxx 参数与账号、2xxx 认证/权限/状态、3xxx 认领、4xxx 文件、5xxx 分类、9999 系统异常。
 * 每个码同时携带建议的 HTTP 状态，由 {@link GlobalExceptionHandler} 统一映射。
 */
public enum ResultCode {

    SUCCESS(200, HttpStatus.OK, "success"),

    PARAM_ERROR(1001, HttpStatus.BAD_REQUEST, "参数不合法"),
    USER_EXISTS(1002, HttpStatus.BAD_REQUEST, "该学号已注册"),
    LOGIN_FAILED(1003, HttpStatus.BAD_REQUEST, "学号或密码错误"),
    USER_DISABLED(1004, HttpStatus.FORBIDDEN, "账号已被禁用，请联系管理员"),
    OLD_PASSWORD_ERROR(1005, HttpStatus.BAD_REQUEST, "原密码错误"),

    UNAUTHORIZED(2001, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"),
    NO_PERMISSION(2002, HttpStatus.FORBIDDEN, "无访问权限"),
    ITEM_STATUS_ILLEGAL(2003, HttpStatus.CONFLICT, "当前状态不允许该操作"),
    NOT_FOUND(2004, HttpStatus.NOT_FOUND, "资源不存在"),
    ITEM_NOT_FOUND(2004, HttpStatus.NOT_FOUND, "信息不存在"),
    USER_NOT_FOUND(2004, HttpStatus.NOT_FOUND, "用户不存在"),
    CATEGORY_NOT_FOUND(5003, HttpStatus.NOT_FOUND, "分类不存在"),

    CLAIM_SELF_NOT_ALLOWED(3001, HttpStatus.BAD_REQUEST, "不能认领自己发布的信息"),
    CLAIM_STATUS_ILLEGAL(3002, HttpStatus.CONFLICT, "该认领申请已处理"),
    CLAIM_EXISTS(3003, HttpStatus.BAD_REQUEST, "已提交过认领申请"),

    FILE_EMPTY(4001, HttpStatus.BAD_REQUEST, "上传文件为空"),
    FILE_TOO_LARGE(4002, HttpStatus.BAD_REQUEST, "图片超过 2MB"),
    FILE_TYPE_ERROR(4003, HttpStatus.BAD_REQUEST, "仅支持 jpg/png 格式"),
    IMAGE_TOO_MANY(4004, HttpStatus.BAD_REQUEST, "最多上传 3 张图片"),

    CATEGORY_EXISTS(5001, HttpStatus.BAD_REQUEST, "分类名称已存在"),
    CATEGORY_IN_USE(5002, HttpStatus.CONFLICT, "分类下存在失物信息，禁止删除"),

    SYSTEM_ERROR(9999, HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误，请稍后重试");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ResultCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
