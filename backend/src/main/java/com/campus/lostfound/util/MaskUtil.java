package com.campus.lostfound.util;

/** 敏感字段掩码（详细设计 8.5）：非本人且非管理员看到 138****6621。 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /** 学号脱敏：2022304512 → 2022****12。 */
    public static String maskStudentNo(String studentNo) {
        if (studentNo == null || studentNo.length() < 7) {
            return studentNo;
        }
        return studentNo.substring(0, 4) + "****" + studentNo.substring(studentNo.length() - 2);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
