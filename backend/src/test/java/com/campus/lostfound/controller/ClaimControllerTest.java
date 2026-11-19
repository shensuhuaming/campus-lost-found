package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.security.JwtInterceptor;
import com.campus.lostfound.service.ClaimService;
import com.campus.lostfound.vo.ClaimAdminVO;
import com.campus.lostfound.vo.ClaimResultVO;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 认领接口切片测试（API 规格 5.1 ~ 5.4）。 */
@WebMvcTest(ClaimController.class)
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimService claimService;

    @MockitoBean
    private JwtInterceptor jwtInterceptor;

    @BeforeEach
    void passInterceptor() throws Exception {
        when(jwtInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("发起认领：成功返回 claimId 与物品状态")
    void create_success() throws Exception {
        when(claimService.create(any())).thenReturn(new ClaimResultVO(55L, 1));

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":101,\"detailDesc\":\"内有一卡通，姓名与我一致\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.claimId").value(55))
                .andExpect(jsonPath("$.data.itemStatus").value(1));
    }

    @Test
    @DisplayName("发起认领：说明为空返回 1001")
    void create_blankDesc_returns1001() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":101,\"detailDesc\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("核验通过：返回 200 与物品已完成状态")
    void approve_success() throws Exception {
        when(claimService.approve(anyLong(), any())).thenReturn(new ClaimResultVO(55L, 2));

        mockMvc.perform(put("/api/claims/55/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reply\":\"请到服务台领取\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemStatus").value(2));
    }

    @Test
    @DisplayName("核验拒绝：回复超长返回 1001")
    void reject_tooLongReply_returns1001() throws Exception {
        mockMvc.perform(put("/api/claims/55/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reply\":\"" + "长".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("核验列表：返回统一分页结构")
    void page_returnsRows() throws Exception {
        ClaimAdminVO vo = new ClaimAdminVO(55L, 101L, "黑色钱包", 1,
                new ClaimAdminVO.Applicant(13L, "王同学", "2022304601"),
                "内有一卡通", 0, "待核验", null, LocalDateTime.now());
        when(claimService.page(any())).thenReturn(new PageResult<>(1, 1, 12, 1, List.of(vo)));

        mockMvc.perform(get("/api/claims").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].applicant.nickname").value("王同学"));
    }
}
