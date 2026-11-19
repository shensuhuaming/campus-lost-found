package com.campus.lostfound.enums;

/** 用户状态：0 正常，1 禁用（数据库 user.status）。 */
public enum UserStatusEnum {

    NORMAL(0, "正常"),
    DISABLED(1, "禁用");

    private final int code;
    private final String desc;

    UserStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
