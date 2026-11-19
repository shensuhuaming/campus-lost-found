package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 我的认领列表项（API 规格 2.8）。 */
public record ClaimVO(
        Long id,
        Long itemId,
        String itemTitle,
        Integer itemStatus,
        String detailDesc,
        Integer status,
        String statusText,
        String reply,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
