package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 列表卡片（API 规格 4.1）。 */
public record ItemCardVO(
        Long id,
        Integer type,
        String typeText,
        String title,
        String coverImage,
        String categoryName,
        String happenPlace,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime happenTime,
        Integer itemStatus,
        String itemStatusText,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
