package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 审核结果（API 规格 7.2）。 */
public record AuditResultVO(
        Long id,
        Integer auditStatus,
        Integer itemStatus,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt) {
}
