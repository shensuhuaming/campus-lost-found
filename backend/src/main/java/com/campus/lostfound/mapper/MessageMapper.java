package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/** 站内消息数据访问。 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
