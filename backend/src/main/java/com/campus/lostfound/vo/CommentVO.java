package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 我的留言列表项（API 规格 2.9）。 */
public record CommentVO(
        Long id,
        Long itemId,
        String itemTitle,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
