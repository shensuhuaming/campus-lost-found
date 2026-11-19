package com.campus.lostfound.controller;

import com.campus.lostfound.security.JwtInterceptor;
import com.campus.lostfound.service.AuthService;
import com.campus.lostfound.vo.LoginVO;
import com.campus.lostfound.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证接口切片测试（API 规格 2.1 ~ 2.3）：路由、参数校验与统一响应结构。
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    /** WebConfig 依赖拦截器 Bean，切片测试中替换掉。 */
    @MockitoBean
    private JwtInterceptor jwtInterceptor;

    /** mock 的拦截器 preHandle 默认返回 false，会把请求全部拦掉，这里显式放行。 */
    @BeforeEach
    void passInterceptor() throws Exception {
        when(jwtInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("注册成功：code=200，data.userId 为新用户 id")
    void register_success() throws Exception {
        when(authService.register(any())).thenReturn(12L);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNo\":\"2022304512\",\"nickname\":\"小李同学\","
                                + "\"phone\":\"13800006621\",\"password\":\"abc123456\","
                                + "\"confirmPassword\":\"abc123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(12));
    }

    @Test
    @DisplayName("注册失败：学号格式非法返回 400 + code=1001")
    void register_invalidStudentNo_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNo\":\"abc\",\"nickname\":\"小李同学\","
                                + "\"phone\":\"13800006621\",\"password\":\"abc123456\","
                                + "\"confirmPassword\":\"abc123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("登录成功：返回 token、tokenType 与用户信息")
    void login_success() throws Exception {
        UserVO user = new UserVO(12L, "2022304512", "小李同学", "13800006621", 0, 0,
                LocalDateTime.of(2026, 3, 1, 9, 12, 0));
        when(authService.login(any())).thenReturn(new LoginVO("jwt-token", "Bearer", 43200L, user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNo\":\"2022304512\",\"password\":\"abc123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(43200))
                .andExpect(jsonPath("$.data.user.studentNo").value("2022304512"));
    }

    @Test
    @DisplayName("登录失败：密码为空返回 400 + code=1001")
    void login_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNo\":\"2022304512\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
