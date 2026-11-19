package com.campus.lostfound.enums;

/** 物品状态：0 进行中，1 处理中，2 已完成，3 已关闭。 */
public enum ItemStatusEnum {

    ONGOING(0, "进行中"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完成"),
    CLOSED(3, "已关闭");

    private final int code;
    private final String text;

    ItemStatusEnum(int code, String text) {
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
        for (ItemStatusEnum e : values()) {
            if (Integer.valueOf(e.code).equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
