package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站内消息（对应 lost_found.message）。from_user_id=0 表示系统。 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long toUserId;

    private Long fromUserId;

    /** 1 审核结果 2 留言提醒 3 认领申请提醒 4 认领结果 5 系统通知 */
    private Integer type;

    private String content;

    private Long itemId;

    /** 0 未读 1 已读 */
    private Integer isRead;

    private LocalDateTime createdAt;
}
