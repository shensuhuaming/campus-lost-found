package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.CommentCreateDTO;
import com.campus.lostfound.entity.Comment;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.AuditStatusEnum;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.mapper.CommentMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.CommentService;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.vo.CommentCreatedVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 留言实现（详细设计 4.5.1、4.5.2）。 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int SUMMARY_LENGTH = 30;

    private final CommentMapper commentMapper;
    private final LostItemMapper itemMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;

    @Override
    @Transactional
    public CommentCreatedVO create(CommentCreateDTO dto) {
        LostItem item = itemMapper.selectById(dto.getItemId());
        if (item == null || !Integer.valueOf(AuditStatusEnum.APPROVED.getCode()).equals(item.getAuditStatus())) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);           // 2004
        }
        Long uid = SecurityUtil.getUserId();
        Comment comment = new Comment();
        comment.setItemId(dto.getItemId());
        comment.setUserId(uid);
        comment.setContent(dto.getContent());
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        if (!item.getUserId().equals(uid)) {                                  // 自己给自己留言不通知
            User commenter = userMapper.selectById(uid);
            String nickname = commenter == null ? "用户" : commenter.getNickname();
            String summary = dto.getContent().length() > SUMMARY_LENGTH
                    ? dto.getContent().substring(0, SUMMARY_LENGTH) + "…" : dto.getContent();
            messageService.push(item.getUserId(), uid, MessageTypeEnum.COMMENT,
                    nickname + " 在「" + item.getTitle() + "」下留言：" + summary, item.getId());
        }
        return new CommentCreatedVO(comment.getId(), comment.getCreatedAt());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);                // 2004
        }
        boolean owner = comment.getUserId().equals(SecurityUtil.getUserId());
        if (!owner && !SecurityUtil.isAdmin()) {
            throw new BusinessException(ResultCode.NO_PERMISSION);            // 2002
        }
        commentMapper.deleteById(id);
    }
}
