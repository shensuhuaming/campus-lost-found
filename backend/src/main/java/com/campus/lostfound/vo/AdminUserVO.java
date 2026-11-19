package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 管理端用户列表项（API 规格 7.7）。 */
public record AdminUserVO(
        Long id,
        String studentNo,
        String nickname,
        String phone,
        Integer role,
        String roleText,
        Integer status,
        String statusText,
        long itemCount,
        long claimCount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
