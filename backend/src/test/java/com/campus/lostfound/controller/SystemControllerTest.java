package com.campus.lostfound.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 切片测试：不起整个 Spring 容器、不连真实数据库，用于验证 HTTP 路由、响应体结构与降级逻辑。
 * 后续业务 Controller 的测试沿用此模式。
 */
@WebMvcTest(SystemController.class)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** 替换真实 JdbcTemplate，避免切片测试依赖 MySQL。 */
    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("数据库可用时：健康检查返回 code=0、status=UP、database.status=UP")
    void health_returnsOk() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);

        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.database.status").value("UP"));
    }

    @Test
    @DisplayName("数据库不可用时：接口仍返回 200，database.status=DOWN（自检降级，不抛 500）")
    void health_databaseDown_degradesGracefully() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenThrow(new RuntimeException("connection refused"));

        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.database.status").value("DOWN"));
    }
}
