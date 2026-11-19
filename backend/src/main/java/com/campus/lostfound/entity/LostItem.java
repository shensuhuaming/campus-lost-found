package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 失物信息（对应 lost_found.lost_item）。 */
@Data
@TableName("lost_item")
public class LostItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 0 寻物，1 拾物 */
    private Integer type;

    private String title;

    private String description;

    private Long categoryId;

    private Long userId;

    private LocalDateTime happenTime;

    private String happenPlace;

    private String contact;

    /** 0 待审核，1 通过，2 驳回 */
    private Integer auditStatus;

    private String rejectReason;

    /** 0 进行中，1 处理中，2 已完成，3 已关闭 */
    private Integer itemStatus;

    /** 关闭/下架理由（管理员下架必填） */
    private String closeReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
