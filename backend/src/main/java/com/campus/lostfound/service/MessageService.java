package com.campus.lostfound.service;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.dto.MessageQuery;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.vo.MessageVO;

/** 站内消息服务（详细设计 4.5.3、4.5.4）。 */
public interface MessageService {

    /** 推送系统消息（from_user_id=0）：当前事务提交后才落库，回滚则不发送。 */
    void push(Long toUserId, MessageTypeEnum type, String content, Long itemId);

    /** 推送带发送人的消息（如留言提醒）。 */
    void push(Long toUserId, Long fromUserId, MessageTypeEnum type, String content, Long itemId);

    /** 我的消息列表（U5.4）：只看自己的，可按已读状态与类型筛选，按时间倒序。 */
    PageResult<MessageVO> page(MessageQuery query);

    /** 标记单条已读：非本人 2002，不存在 2004，重复调用幂等。 */
    void markRead(Long id);

    /** 未读消息数。 */
    long unreadCount();
}
