package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.entity.LostItem;
import org.apache.ibatis.annotations.Mapper;

/** 失物信息数据访问。 */
@Mapper
public interface LostItemMapper extends BaseMapper<LostItem> {
}
