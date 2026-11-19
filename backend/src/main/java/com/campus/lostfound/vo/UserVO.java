package com.campus.lostfound.vo;

import com.campus.lostfound.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 用户视图对象：不含 password 字段（详细设计 8.5）。 */
public record UserVO(
        Long id,
        String studentNo,
        String nickname,
        String phone,
        Integer role,
        Integer status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {

    public static UserVO of(User user) {
        return new UserVO(user.getId(), user.getStudentNo(), user.getNickname(),
                user.getPhone(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
