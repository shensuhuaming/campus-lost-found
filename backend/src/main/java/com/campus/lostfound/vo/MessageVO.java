package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 站内消息（API 规格 6.3）。 */
public record MessageVO(
        Long id,
        Integer type,
        String typeText,
        String content,
        Long itemId,
        Long fromUserId,
        String fromNickname,
        Integer isRead,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
