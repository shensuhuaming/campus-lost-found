package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/** 待审核列表项（API 规格 7.1）。 */
public record AuditItemVO(
        Long id,
        String title,
        Integer type,
        String typeText,
        Long categoryId,
        String categoryName,
        String publisherNickname,
        String description,
        List<String> images,
        Integer auditStatus,
        String auditStatusText,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
