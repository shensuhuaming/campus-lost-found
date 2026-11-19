package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.security.JwtInterceptor;
import com.campus.lostfound.service.UserService;
import com.campus.lostfound.vo.ClaimVO;
import com.campus.lostfound.vo.ItemListVO;
import com.campus.lostfound.vo.ProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 个人中心接口切片测试（API 规格 2.4 ~ 2.9）。 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtInterceptor jwtInterceptor;

    @BeforeEach
    void passInterceptor() throws Exception {
        when(jwtInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/user/profile：返回用户信息与四项统计")
    void profile_returnsStats() throws Exception {
        when(userService.getProfile()).thenReturn(new ProfileVO(12L, "2022304512", "小李同学",
                "13800006621", 0, LocalDateTime.of(2026, 3, 1, 9, 12), new ProfileVO.Stats(6, 3, 1, 8)));

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.studentNo").value("2022304512"))
                .andExpect(jsonPath("$.data.stat.published").value(6))
                .andExpect(jsonPath("$.data.stat.claimingPending").value(1));
    }

    @Test
    @DisplayName("PUT /api/user/password：新密码过短返回 400 + code=1001")
    void changePassword_tooShort_returns400() throws Exception {
        mockMvc.perform(put("/api/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"abc123456\",\"newPassword\":\"123\",\"confirmPassword\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("PUT /api/user/profile：昵称非法返回 400 + code=1001")
    void updateProfile_invalidNickname_returns400() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"x\",\"phone\":\"13800006621\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("GET /api/user/items：返回统一分页结构")
    void myItems_returnsPage() throws Exception {
        ItemListVO vo = new ItemListVO(101L, 1, "黑色钱包", "证件", "/uploads/a1.png",
                1, "已通过", null, 1, "处理中", 2, LocalDateTime.now());
        when(userService.myItems(any(), any(), any()))
                .thenReturn(new PageResult<>(1, 1, 12, 1, List.of(vo)));

        mockMvc.perform(get("/api/user/items").param("page", "1").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("黑色钱包"));
    }

    @Test
    @DisplayName("GET /api/user/claims：按状态筛选参数透传")
    void myClaims_returnsPage() throws Exception {
        ClaimVO vo = new ClaimVO(55L, 101L, "黑色钱包", 2, "内有一卡通", 1, "已通过", "已核实", LocalDateTime.now());
        when(userService.myClaims(any(), anyInt()))
                .thenReturn(new PageResult<>(1, 1, 12, 1, List.of(vo)));

        mockMvc.perform(get("/api/user/claims").param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].statusText").value("已通过"));
    }
}
