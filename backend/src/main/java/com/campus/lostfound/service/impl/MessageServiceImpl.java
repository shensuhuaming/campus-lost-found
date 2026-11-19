package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.MessageQuery;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.mapper.MessageMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 站内消息实现（详细设计 4.5.3、4.5.4，评审 P0-6）。
 * <p>
 * 推送：存在事务时注册 {@code afterCommit} 回调，业务提交成功才通知，回滚则不发。
 * 读取：只看自己的消息；已读与未读数均按当前用户过滤。
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);
    private static final long SYSTEM_USER_ID = 0L;
    private static final String SYSTEM_NICKNAME = "系统";

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @Override
    public void push(Long toUserId, MessageTypeEnum type, String content, Long itemId) {
        push(toUserId, SYSTEM_USER_ID, type, content, itemId);
    }

    @Override
    public void push(Long toUserId, Long fromUserId, MessageTypeEnum type, String content, Long itemId) {
        if (toUserId == null) {
            return;
        }
        long from = fromUserId == null ? SYSTEM_USER_ID : fromUserId;
        Runnable insert = () -> {
            try {
                Message message = new Message();
                message.setToUserId(toUserId);
                message.setFromUserId(from);
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

    @Override
    public PageResult<MessageVO> page(MessageQuery query) {
        Long uid = SecurityUtil.getUserId();
        IPage<Message> page = messageMapper.selectPage(new Page<>(query.safePage(), query.safeSize()),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getToUserId, uid)
                        .eq(query.getIsRead() != null, Message::getIsRead, query.getIsRead())
                        .eq(query.getType() != null, Message::getType, query.getType())
                        .orderByDesc(Message::getCreatedAt));
        List<Message> records = page.getRecords();
        if (records.isEmpty()) {
            return PageResult.empty(query.safePage(), query.safeSize());
        }

        Set<Long> fromIds = records.stream().map(Message::getFromUserId)
                .filter(id -> id != null && id != SYSTEM_USER_ID).collect(Collectors.toSet());
        Map<Long, User> senders = fromIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(fromIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<MessageVO> vos = records.stream().map(m -> {
            Long fromId = m.getFromUserId();
            boolean system = fromId == null || fromId == SYSTEM_USER_ID;
            String fromNickname = system ? SYSTEM_NICKNAME
                    : (senders.containsKey(fromId) ? senders.get(fromId).getNickname() : null);
            return new MessageVO(m.getId(), m.getType(), MessageTypeEnum.textOf(m.getType()),
                    m.getContent(), m.getItemId(), fromId, fromNickname, m.getIsRead(), m.getCreatedAt());
        }).toList();
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(),
                (int) page.getPages(), vos);
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        Message message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);                // 2004
        }
        if (!message.getToUserId().equals(SecurityUtil.getUserId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);            // 2002
        }
        if (Integer.valueOf(1).equals(message.getIsRead())) {
            return;                                                          // 幂等
        }
        message.setIsRead(1);
        messageMapper.updateById(message);
    }

    @Override
    public long unreadCount() {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, SecurityUtil.getUserId())
                .eq(Message::getIsRead, 0));
    }
}
