package com.campus.lostfound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 校园失物招领系统后端启动类。
 * <p>
 * 当前为环境骨架版本：只包含健康检查接口与统一响应/异常处理，
 * 业务模块（用户、物品、认领、留言、管理后台）待详细设计与接口契约确认后实现。
 */
@SpringBootApplication
public class LostFoundApplication {

    public static void main(String[] args) {
        SpringApplication.run(LostFoundApplication.class, args);
    }
}
