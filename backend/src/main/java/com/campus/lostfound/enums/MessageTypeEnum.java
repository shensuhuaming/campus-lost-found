package com.campus.lostfound.enums;

/** 消息类型（API 规格 1.5）：1 审核结果 2 留言提醒 3 认领申请提醒 4 认领结果 5 系统通知。 */
public enum MessageTypeEnum {

    AUDIT(1, "审核结果"),
    COMMENT(2, "留言提醒"),
    CLAIM_APPLY(3, "认领申请提醒"),
    CLAIM_RESULT(4, "认领结果"),
    SYSTEM(5, "系统通知");

    private final int code;
    private final String text;

    MessageTypeEnum(int code, String text) {
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
        for (MessageTypeEnum e : values()) {
            if (Integer.valueOf(e.code).equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
