package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.AdminUserQuery;
import com.campus.lostfound.dto.AuditDTO;
import com.campus.lostfound.dto.CategoryDTO;
import com.campus.lostfound.dto.UserStatusDTO;
import com.campus.lostfound.service.AdminService;
import com.campus.lostfound.vo.AdminUserVO;
import com.campus.lostfound.vo.AuditItemVO;
import com.campus.lostfound.vo.AuditResultVO;
import com.campus.lostfound.vo.CategoryAdminVO;
import com.campus.lostfound.vo.StatsVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 管理后台接口（API 规格 7.1 ~ 7.9）。 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ---------------- U6.1 信息审核 ----------------

    /** 待审核列表，默认待审核；可传 1/2 查看历史。 */
    @GetMapping("/items/audit")
    public Result<PageResult<AuditItemVO>> auditPage(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "12") int size,
                                                     @RequestParam(required = false) Integer auditStatus,
                                                     @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.auditPage(page, size, auditStatus, keyword));
    }

    /** 审核：1 通过、2 驳回（驳回必填理由）。 */
    @PutMapping("/items/{id}/audit")
    public Result<AuditResultVO> audit(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        return Result.ok(adminService.audit(id, dto));
    }

    // ---------------- U6.2 分类管理 ----------------

    @GetMapping("/categories")
    public Result<List<CategoryAdminVO>> categories() {
        return Result.ok(adminService.categories());
    }

    @PostMapping("/categories")
    public Result<CategoryAdminVO> createCategory(@Valid @RequestBody CategoryDTO dto) {
        return Result.ok(adminService.createCategory(dto));
    }

    @PutMapping("/categories/{id}")
    public Result<CategoryAdminVO> updateCategory(@PathVariable Long id,
                                                  @Valid @RequestBody CategoryDTO dto) {
        return Result.ok(adminService.updateCategory(id, dto));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return Result.ok();
    }

    // ---------------- U6.3 用户管理 ----------------

    @GetMapping("/users")
    public Result<PageResult<AdminUserVO>> users(AdminUserQuery query) {
        return Result.ok(adminService.users(query));
    }

    @PutMapping("/users/{id}/status")
    public Result<Map<String, Object>> updateUserStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UserStatusDTO dto) {
        adminService.updateUserStatus(id, dto.getStatus());
        return Result.ok(Map.of("id", id, "status", dto.getStatus()));
    }

    // ---------------- U6.4 数据统计 ----------------

    @GetMapping("/stats")
    public Result<StatsVO> stats(@RequestParam(required = false) Integer days) {
        return Result.ok(adminService.stats(days));
    }
}
