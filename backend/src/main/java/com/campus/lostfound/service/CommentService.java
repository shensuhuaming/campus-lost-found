package com.campus.lostfound.service;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.dto.CommentCreateDTO;
import com.campus.lostfound.dto.MessageQuery;
import com.campus.lostfound.vo.CommentCreatedVO;
import com.campus.lostfound.vo.MessageVO;

/** 留言与站内消息服务（详细设计 4.5）。 */
public interface CommentService {

    /** 发表留言（U5.1）：通知发布者（自己给自己留言不通知）。 */
    CommentCreatedVO create(CommentCreateDTO dto);

    /** 删除留言（U5.2）：留言本人或管理员。 */
    void delete(Long id);
}
