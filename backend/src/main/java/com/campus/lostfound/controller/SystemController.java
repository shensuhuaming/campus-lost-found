package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统自检接口，仅用于环境验证与健康检查，不含业务逻辑。
 * <p>
 * 前端首页会调用它展示「后端是否连通、数据库是否连通」，
 * 测试同学部署后可据此快速判断环境是否就绪。
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name:campus-lost-found}")
    private String appName;

    public SystemController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", appName);
        body.put("status", "UP");
        body.put("javaVersion", System.getProperty("java.version"));
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("database", checkDatabase());
        return ApiResponse.ok(body);
    }

    /** 数据库连通性：执行 SELECT 1；异常不抛出，避免自检接口本身返回 500。 */
    private Map<String, Object> checkDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            db.put("status", one != null && one == 1 ? "UP" : "UNKNOWN");
        } catch (Exception e) {
            log.warn("数据库连通性检查失败: {}", e.getMessage());
            db.put("status", "DOWN");
            db.put("error", e.getClass().getSimpleName());
        }
        return db;
    }
}
