package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 我的发布列表项（API 规格 2.7）。 */
public record ItemListVO(
        Long id,
        Integer type,
        String title,
        String categoryName,
        String coverImage,
        Integer auditStatus,
        String auditStatusText,
        String rejectReason,
        Integer itemStatus,
        String itemStatusText,
        long claimCount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
