package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** 用户表数据访问（MyBatis-Plus BaseMapper，单表 CRUD 不写 XML）。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
