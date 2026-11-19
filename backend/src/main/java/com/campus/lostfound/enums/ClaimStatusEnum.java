package com.campus.lostfound.enums;

/** 认领申请状态：0 待核验，1 通过，2 拒绝。 */
public enum ClaimStatusEnum {

    PENDING(0, "待核验"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String text;

    ClaimStatusEnum(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static String textOf(Integer code) {
        for (ClaimStatusEnum e : values()) {
            if (Integer.valueOf(e.code).equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
