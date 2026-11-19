package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 认领申请（对应 lost_found.claim）。 */
@Data
@TableName("claim")
public class Claim {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;

    private Long applicantId;

    private String detailDesc;

    /** 0 待核验，1 通过，2 拒绝 */
    private Integer status;

    private String reply;

    private Long adminId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
