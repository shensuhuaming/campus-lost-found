package com.campus.lostfound.enums;

/** 用户角色：0 学生，1 管理员（数据库 user.role）。 */
public enum RoleEnum {

    STUDENT(0, "学生"),
    ADMIN(1, "管理员");

    private final int code;
    private final String desc;

    RoleEnum(int code, String desc) {
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
