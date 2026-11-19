package com.campus.lostfound.service.impl;

import com.campus.lostfound.entity.Message;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.mapper.MessageMapper;
import com.campus.lostfound.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 消息推送实现（详细设计 4.5.3、评审 P0-6）。
 * <p>
 * 存在事务时注册 {@code afterCommit} 回调：业务提交成功才通知，回滚则不发，避免"业务失败却收到成功通知"。
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);
    private static final long SYSTEM_USER_ID = 0L;

    private final MessageMapper messageMapper;

    @Override
    public void push(Long toUserId, MessageTypeEnum type, String content, Long itemId) {
        if (toUserId == null) {
            return;
        }
        Runnable insert = () -> {
            try {
                Message message = new Message();
                message.setToUserId(toUserId);
                message.setFromUserId(SYSTEM_USER_ID);
                message.setType(type.getCode());
                message.setContent(content);
                message.setItemId(itemId);
                message.setIsRead(0);
                message.setCreatedAt(LocalDateTime.now());
                messageMapper.insert(message);
            } catch (Exception e) {
                log.error("push message failed, to={}, type={}", toUserId, type, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    insert.run();
                }
            });
        } else {
            insert.run();
        }
    }
}
