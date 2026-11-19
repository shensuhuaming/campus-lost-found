package com.campus.lostfound.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** 扫描 Mapper 接口（详细设计 2.3：mapper 包下继承 BaseMapper 的接口）。 */
@Configuration
@MapperScan("com.campus.lostfound.mapper")
public class MybatisPlusConfig {
}
