package com.campus.lostfound.enums;

/** 审核状态：0 待审核，1 通过，2 驳回。 */
public enum AuditStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回");

    private final int code;
    private final String text;

    AuditStatusEnum(int code, String text) {
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
        for (AuditStatusEnum e : values()) {
            if (Integer.valueOf(e.code).equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
