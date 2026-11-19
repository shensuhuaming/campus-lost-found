package com.campus.lostfound.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 认领核验列表的行（三表关联查询的映射对象）。 */
@Data
public class ClaimAdminRow {

    private Long id;
    private Long itemId;
    private String itemTitle;
    private Integer itemType;
    private Long applicantId;
    private String applicantNickname;
    private String applicantStudentNo;
    private String detailDesc;
    private Integer status;
    private String reply;
    private LocalDateTime createdAt;
}
