package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.security.JwtInterceptor;
import com.campus.lostfound.service.CommentService;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.vo.CommentCreatedVO;
import com.campus.lostfound.vo.MessageVO;
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

/** 留言与消息接口切片测试（API 规格 6.1 ~ 6.5）。 */
@WebMvcTest({CommentController.class, MessageController.class})
class CommentMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JwtInterceptor jwtInterceptor;

    @BeforeEach
    void passInterceptor() throws Exception {
        when(jwtInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("发表留言：成功返回 id 与时间")
    void createComment_success() throws Exception {
        when(commentService.create(any())).thenReturn(new CommentCreatedVO(301L, LocalDateTime.now()));

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":101,\"content\":\"这个钱包好像是隔壁宿舍同学的\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(301));
    }

    @Test
    @DisplayName("发表留言：内容为空返回 1001")
    void createComment_blank_returns1001() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":101,\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("消息列表：返回分页与类型文案")
    void pageMessages_returnsRows() throws Exception {
        MessageVO vo = new MessageVO(900L, 3, "认领申请提醒", "王同学申请认领", 101L,
                0L, "系统", 0, LocalDateTime.now());
        when(messageService.page(any())).thenReturn(new PageResult<>(1, 1, 10, 1, List.of(vo)));

        mockMvc.perform(get("/api/messages").param("isRead", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].typeText").value("认领申请提醒"));
    }

    @Test
    @DisplayName("标记已读：返回 id 与 isRead=1")
    void markRead_returnsOk() throws Exception {
        mockMvc.perform(put("/api/messages/900/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(1));
    }

    @Test
    @DisplayName("未读数：返回 count")
    void unreadCount_returnsCount() throws Exception {
        when(messageService.unreadCount()).thenReturn(3L);

        mockMvc.perform(get("/api/messages/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
    }
}
