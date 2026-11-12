package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统自检接口，仅用于环境验证与健康检查，不含业务逻辑。
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Value("${spring.application.name:campus-lost-found}")
    private String appName;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", appName);
        body.put("status", "UP");
        body.put("javaVersion", System.getProperty("java.version"));
        body.put("timestamp", OffsetDateTime.now().toString());
        return ApiResponse.ok(body);
    }
}
