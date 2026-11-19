package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/** 信息详情（API 规格 4.2）：图片、分类、发布者、留言、我的认领状态；联系方式按登录态掩码。 */
public record ItemDetailVO(
        Long id,
        Integer type,
        String typeText,
        String title,
        String description,
        Long categoryId,
        String categoryName,
        String contact,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime happenTime,
        String happenPlace,
        Integer auditStatus,
        String auditStatusText,
        Integer itemStatus,
        String itemStatusText,
        String closeReason,
        Publisher publisher,
        List<String> images,
        List<CommentBriefVO> comments,
        Integer myClaimStatus,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {

    /** 发布者公开信息（学号脱敏）。 */
    public record Publisher(Long id, String nickname, String studentNo) {
    }
}
