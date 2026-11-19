package com.campus.lostfound.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 待审核列表行（物品 ⊕ 发布者 ⊕ 分类）。 */
@Data
public class AuditRow {

    private Long id;
    private String title;
    private Integer type;
    private Long categoryId;
    private String categoryName;
    private String publisherNickname;
    private String description;
    private Integer auditStatus;
    private Integer itemStatus;
    private LocalDateTime createdAt;
}
