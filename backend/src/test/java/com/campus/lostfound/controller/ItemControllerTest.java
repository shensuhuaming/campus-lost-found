package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.config.JacksonConfig;
import com.campus.lostfound.security.JwtInterceptor;
import com.campus.lostfound.service.ItemService;
import com.campus.lostfound.vo.ItemCardVO;
import com.campus.lostfound.vo.ItemDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
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

/** 失物信息与浏览检索接口切片测试（API 规格 3.1~3.3、4.1~4.2）。 */
@WebMvcTest(ItemController.class)
@Import(JacksonConfig.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private JwtInterceptor jwtInterceptor;

    @BeforeEach
    void passInterceptor() throws Exception {
        when(jwtInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("发布成功：返回 code=200 与 data.id")
    void create_success() throws Exception {
        when(itemService.create(any())).thenReturn(101L);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":1,\"title\":\"黑色钱包\","
                                + "\"description\":\"图书馆三楼捡到黑色钱包，内有一卡通\","
                                + "\"categoryId\":1,\"happenTime\":\"2026-11-19 10:00:00\","
                                + "\"happenPlace\":\"图书馆\",\"contact\":\"13800006621\",\"imageUrls\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(101));
    }

    @Test
    @DisplayName("发布失败：标题超长返回 1001")
    void create_titleTooLong_returns1001() throws Exception {
        String longTitle = "长".repeat(31);
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":1,\"title\":\"" + longTitle + "\","
                                + "\"description\":\"图书馆三楼捡到黑色钱包，内有一卡通\","
                                + "\"categoryId\":1,\"happenTime\":\"2026-11-19 10:00:00\","
                                + "\"happenPlace\":\"图书馆\",\"contact\":\"13800006621\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("列表：返回统一分页结构与卡片字段")
    void page_returnsCards() throws Exception {
        ItemCardVO card = new ItemCardVO(101L, 1, "拾物", "黑色钱包", null, "证件",
                "图书馆", LocalDateTime.now(), 0, "进行中", LocalDateTime.now());
        when(itemService.page(any())).thenReturn(new PageResult<>(1, 1, 12, 1, List.of(card)));

        mockMvc.perform(get("/api/items").param("page", "1").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("黑色钱包"));
    }

    @Test
    @DisplayName("详情：返回信息与发布者")
    void detail_returnsItem() throws Exception {
        ItemDetailVO vo = new ItemDetailVO(101L, 1, "拾物", "黑色钱包", "描述", 1L, "证件",
                "138****6621", LocalDateTime.now(), "图书馆", 1, "已通过", 0, "进行中", null,
                new ItemDetailVO.Publisher(12L, "小李同学", "2022****12"), List.of(), List.of(),
                null, LocalDateTime.now());
        when(itemService.detail(101L)).thenReturn(vo);

        mockMvc.perform(get("/api/items/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("黑色钱包"))
                .andExpect(jsonPath("$.data.publisher.nickname").value("小李同学"))
                .andExpect(jsonPath("$.data.contact").value("138****6621"));
    }

    @Test
    @DisplayName("关闭：调用服务并返回 200")
    void close_success() throws Exception {
        mockMvc.perform(put("/api/items/101/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"信息不实\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("寻物推进：targetStatus 缺失返回 1001")
    void advance_missingTarget_returns1001() throws Exception {
        mockMvc.perform(put("/api/items/101/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }
}
