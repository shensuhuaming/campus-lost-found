package com.campus.lostfound.service;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.dto.AdminUserQuery;
import com.campus.lostfound.dto.AuditDTO;
import com.campus.lostfound.dto.CategoryDTO;
import com.campus.lostfound.vo.AdminUserVO;
import com.campus.lostfound.vo.AuditItemVO;
import com.campus.lostfound.vo.AuditResultVO;
import com.campus.lostfound.vo.CategoryAdminVO;
import com.campus.lostfound.vo.StatsVO;

import java.util.List;

/** 管理后台服务（详细设计 4.6）：审核、分类、用户、统计。 */
public interface AdminService {

    /** 待审核列表（U6.1）。 */
    PageResult<AuditItemVO> auditPage(int page, int size, Integer auditStatus, String keyword);

    /** 审核通过/驳回（U6.1），驳回必填原因。 */
    AuditResultVO audit(Long id, AuditDTO dto);

    /** 分类列表（U6.2），含信息数。 */
    List<CategoryAdminVO> categories();

    CategoryAdminVO createCategory(CategoryDTO dto);

    CategoryAdminVO updateCategory(Long id, CategoryDTO dto);

    void deleteCategory(Long id);

    /** 用户列表（U6.3）。 */
    PageResult<AdminUserVO> users(AdminUserQuery query);

    /** 启用/禁用用户（U1.7、U6.3）。 */
    void updateUserStatus(Long id, Integer status);

    /** 数据统计（U6.4）。 */
    StatsVO stats(Integer days);
}
