package com.campus.lostfound.enums;

/** 物品类型：0 寻物，1 拾物。 */
public enum ItemTypeEnum {

    LOST(0, "寻物"),
    FOUND(1, "拾物");

    private final int code;
    private final String text;

    ItemTypeEnum(int code, String text) {
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
        for (ItemTypeEnum e : values()) {
            if (Integer.valueOf(e.code).equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
