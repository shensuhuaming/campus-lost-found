package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/** 留言数据访问。 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
