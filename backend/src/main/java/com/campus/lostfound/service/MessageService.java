package com.campus.lostfound.service;

import com.campus.lostfound.enums.MessageTypeEnum;

/** 站内消息推送（详细设计 4.5.3）：内部方法，不对外暴露 HTTP。 */
public interface MessageService {

    /**
     * 推送一条消息。当前事务提交后才真正落库，回滚则不发送（避免幽灵通知）。
     * 失败只记日志，不影响主业务。
     */
    void push(Long toUserId, MessageTypeEnum type, String content, Long itemId);
}
