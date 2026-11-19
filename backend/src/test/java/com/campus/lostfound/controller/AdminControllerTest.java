package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.security.JwtInterceptor;
import com.campus.lostfound.service.AdminService;
import com.campus.lostfound.vo.AuditItemVO;
import com.campus.lostfound.vo.AuditResultVO;
import com.campus.lostfound.vo.CategoryAdminVO;
import com.campus.lostfound.vo.StatsVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 管理后台接口切片测试（API 规格 7.1 ~ 7.9）。 */
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtInterceptor jwtInterceptor;

    @BeforeEach
    void passInterceptor() throws Exception {
        when(jwtInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("待审核列表：返回分页与图片")
    void auditPage_returnsRows() throws Exception {
        AuditItemVO vo = new AuditItemVO(101L, "黑色钱包", 1, "拾物", 1L, "证件", "小李同学",
                "描述", List.of("/uploads/202611/a1.png"), 0, "待审核", LocalDateTime.now());
        when(adminService.auditPage(1, 12, null, null))
                .thenReturn(new PageResult<>(1, 1, 12, 1, List.of(vo)));

        mockMvc.perform(get("/api/admin/items/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].publisherNickname").value("小李同学"));
    }

    @Test
    @DisplayName("审核：auditStatus 非法（3）返回 1001")
    void audit_invalidStatus_returns1001() throws Exception {
        mockMvc.perform(put("/api/admin/items/101/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditStatus\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("审核：通过返回 200 与新状态")
    void audit_success() throws Exception {
        when(adminService.audit(anyLong(), any()))
                .thenReturn(new AuditResultVO(101L, 1, 0, LocalDateTime.now()));

        mockMvc.perform(put("/api/admin/items/101/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditStatus\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value(1));
    }

    @Test
    @DisplayName("新增分类：名称为空返回 1001")
    void createCategory_blank_returns1001() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"sort\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("分类列表：返回 itemCount")
    void categories_returnsCount() throws Exception {
        when(adminService.categories()).thenReturn(List.of(new CategoryAdminVO(1L, "证件", 1, 6L)));

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemCount").value(6));
    }

    @Test
    @DisplayName("删除分类：返回 200")
    void deleteCategory_success() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("统计：返回四项指标与趋势")
    void stats_returnsMetrics() throws Exception {
        StatsVO stats = new StatsVO(10, 3, 8, 66.7,
                List.of(new StatsVO.CategoryStat(1L, "证件", 6, 4, 66.7)),
                List.of(new StatsVO.TrendPoint("2026-11-19", 2)));
        when(adminService.stats(null)).thenReturn(stats);

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(10))
                .andExpect(jsonPath("$.data.returnRate").value(66.7))
                .andExpect(jsonPath("$.data.trend[0].count").value(2));
    }
}
