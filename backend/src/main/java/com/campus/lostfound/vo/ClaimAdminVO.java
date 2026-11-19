package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 核验列表项（API 规格 5.2）：含物品标题与申请人信息。 */
public record ClaimAdminVO(
        Long id,
        Long itemId,
        String itemTitle,
        Integer itemType,
        Applicant applicant,
        String detailDesc,
        Integer status,
        String statusText,
        String reply,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {

    public record Applicant(Long id, String nickname, String studentNo) {
    }
}
