package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.AdminUserQuery;
import com.campus.lostfound.dto.AuditDTO;
import com.campus.lostfound.dto.AuditRow;
import com.campus.lostfound.dto.CategoryDTO;
import com.campus.lostfound.dto.CategoryStatRow;
import com.campus.lostfound.dto.DailyCountRow;
import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.ItemImage;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemImageMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.LoginUser;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.vo.AdminUserVO;
import com.campus.lostfound.vo.AuditItemVO;
import com.campus.lostfound.vo.AuditResultVO;
import com.campus.lostfound.vo.CategoryAdminVO;
import com.campus.lostfound.vo.StatsVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** U6.1~U6.4 管理后台单元测试（详细设计 4.6）。 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    static {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> clazz : new Class<?>[]{LostItem.class, ItemImage.class, Claim.class,
                Category.class, User.class}) {
            TableInfoHelper.initTableInfo(assistant, clazz);
        }
    }

    @Mock
    private LostItemMapper itemMapper;
    @Mock
    private ItemImageMapper imageMapper;
    @Mock
    private ClaimMapper claimMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MessageService messageService;
    @InjectMocks
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        SecurityUtil.set(new LoginUser(1L, 1));
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    // ---------------- U6.1 审核 ----------------

    @Test
    @DisplayName("待审核列表：非管理员返回 2002")
    void auditPage_nonAdmin_returns2002() {
        SecurityUtil.set(new LoginUser(13L, 0));

        assertThatThrownBy(() -> adminService.auditPage(1, 12, 0, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NO_PERMISSION);
    }

    @Test
    @DisplayName("待审核列表：默认待审核并组装图片与分类名")
    void auditPage_returnsRows() {
        AuditRow row = new AuditRow();
        row.setId(101L);
        row.setTitle("黑色钱包");
        row.setType(1);
        row.setCategoryId(1L);
        row.setCategoryName("证件");
        row.setPublisherNickname("小李同学");
        row.setAuditStatus(0);
        row.setCreatedAt(LocalDateTime.now());
        when(itemMapper.pageForAudit(any(), eq(0), any())).thenAnswer(inv -> {
            IPage<AuditRow> page = inv.getArgument(0);
            page.setRecords(List.of(row));
            page.setTotal(1);
            return page;
        });
        ItemImage image = new ItemImage();
        image.setItemId(101L);
        image.setUrl("/uploads/202611/a1.png");
        image.setSort(0);
        when(imageMapper.selectList(any())).thenReturn(List.of(image));

        PageResult<AuditItemVO> result = adminService.auditPage(1, 12, null, null);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records().get(0).images()).hasSize(1);
        assertThat(result.records().get(0).typeText()).isEqualTo("拾物");
    }

    @Test
    @DisplayName("审核通过：写 audit_status=1 并通知发布者")
    void audit_approve_success() {
        when(itemMapper.selectById(101L)).thenReturn(item(0));
        when(itemMapper.update(any(), any())).thenReturn(1);

        AuditResultVO result = adminService.audit(101L, dto(1, null));

        assertThat(result.auditStatus()).isEqualTo(1);
        verify(messageService).push(eq(12L), eq(MessageTypeEnum.AUDIT), any(), eq(101L));
    }

    @Test
    @DisplayName("审核驳回：未填原因返回 1001")
    void audit_rejectWithoutReason_returns1001() {
        when(itemMapper.selectById(101L)).thenReturn(item(0));

        assertThatThrownBy(() -> adminService.audit(101L, dto(2, "  ")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR);
    }

    @Test
    @DisplayName("审核：已审核过的信息返回 2003（重复审核）")
    void audit_alreadyAudited_returns2003() {
        LostItem audited = item(0);
        audited.setAuditStatus(1);
        when(itemMapper.selectById(101L)).thenReturn(audited);

        assertThatThrownBy(() -> adminService.audit(101L, dto(1, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_STATUS_ILLEGAL);
    }

    @Test
    @DisplayName("审核：已关闭信息返回 2003")
    void audit_closedItem_returns2003() {
        LostItem closed = item(0);
        closed.setItemStatus(3);
        when(itemMapper.selectById(101L)).thenReturn(closed);

        assertThatThrownBy(() -> adminService.audit(101L, dto(1, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_STATUS_ILLEGAL);
    }

    @Test
    @DisplayName("审核：信息不存在返回 2004")
    void audit_notFound_returns2004() {
        when(itemMapper.selectById(101L)).thenReturn(null);

        assertThatThrownBy(() -> adminService.audit(101L, dto(1, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_NOT_FOUND);
    }

    // ---------------- U6.2 分类 ----------------

    @Test
    @DisplayName("新增分类：重名返回 5001")
    void createCategory_duplicate_returns5001() {
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> adminService.createCategory(categoryDTO("证件")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CATEGORY_EXISTS);
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    @DisplayName("新增分类：成功返回带 id 的对象")
    void createCategory_success() {
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(categoryMapper.insert(any(Category.class))).thenAnswer(inv -> {
            ((Category) inv.getArgument(0)).setId(9L);
            return 1;
        });

        CategoryAdminVO vo = adminService.createCategory(categoryDTO("钥匙"));

        assertThat(vo.id()).isEqualTo(9L);
        assertThat(vo.name()).isEqualTo("钥匙");
    }

    @Test
    @DisplayName("修改分类：不存在返回 5003")
    void updateCategory_notFound_returns5003() {
        when(categoryMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> adminService.updateCategory(9L, categoryDTO("钥匙")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("修改分类：与其他分类重名返回 5001")
    void updateCategory_duplicate_returns5001() {
        Category category = new Category();
        category.setId(9L);
        category.setName("证件");
        when(categoryMapper.selectById(9L)).thenReturn(category);
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> adminService.updateCategory(9L, categoryDTO("电子产品")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CATEGORY_EXISTS);
    }

    @Test
    @DisplayName("删除分类：仍有信息引用返回 5002 并提示数量")
    void deleteCategory_inUse_returns5002() {
        when(categoryMapper.selectById(9L)).thenReturn(new Category());
        when(itemMapper.selectCount(any(Wrapper.class))).thenReturn(3L);

        assertThatThrownBy(() -> adminService.deleteCategory(9L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResultCode.CATEGORY_IN_USE);
                    assertThat(be.getMessage()).contains("3");
                });
        verify(categoryMapper, never()).deleteById(9L);
    }

    @Test
    @DisplayName("删除分类：无引用可删除")
    void deleteCategory_success() {
        when(categoryMapper.selectById(9L)).thenReturn(new Category());
        when(itemMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        adminService.deleteCategory(9L);

        verify(categoryMapper).deleteById(9L);
    }

    // ---------------- U6.3 用户 ----------------

    @Test
    @DisplayName("用户列表：非管理员返回 2002")
    void users_nonAdmin_returns2002() {
        SecurityUtil.set(new LoginUser(13L, 0));

        assertThatThrownBy(() -> adminService.users(new AdminUserQuery()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NO_PERMISSION);
    }

    @Test
    @DisplayName("用户列表：带发布数与认领数")
    void users_returnsCounts() {
        User user = user(12L, 0, 0);
        when(userMapper.selectPage(any(), any())).thenAnswer(inv -> {
            IPage<User> page = inv.getArgument(0);
            page.setRecords(List.of(user));
            page.setTotal(1);
            return page;
        });
        Map<String, Object> itemRow = new HashMap<>();
        itemRow.put("user_id", 12L);
        itemRow.put("cnt", 6L);
        when(itemMapper.selectMaps(any())).thenReturn(List.of(itemRow));
        Map<String, Object> claimRow = new HashMap<>();
        claimRow.put("applicant_id", 12L);
        claimRow.put("cnt", 3L);
        when(claimMapper.selectMaps(any())).thenReturn(List.of(claimRow));

        PageResult<AdminUserVO> result = adminService.users(new AdminUserQuery());

        assertThat(result.records().get(0).itemCount()).isEqualTo(6);
        assertThat(result.records().get(0).claimCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("禁用用户：不允许操作自己返回 2002")
    void updateUserStatus_self_returns2002() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, 0, 0));

        assertThatThrownBy(() -> adminService.updateUserStatus(1L, 1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NO_PERMISSION);
    }

    @Test
    @DisplayName("禁用用户：不允许禁用其他管理员返回 2002")
    void updateUserStatus_adminTarget_returns2002() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 0));

        assertThatThrownBy(() -> adminService.updateUserStatus(2L, 1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NO_PERMISSION);
    }

    @Test
    @DisplayName("禁用用户：普通学生可禁用")
    void updateUserStatus_success() {
        when(userMapper.selectById(12L)).thenReturn(user(12L, 0, 0));
        when(userMapper.update(any(), any())).thenReturn(1);

        adminService.updateUserStatus(12L, 1);

        verify(userMapper).update(any(), any());
    }

    // ---------------- U6.4 统计 ----------------

    @Test
    @DisplayName("统计：归还率 = 已完成拾物/已过审拾物，趋势按天补零")
    void stats_computesRatesAndTrend() {
        when(itemMapper.selectCount(any())).thenReturn(10L, 3L, 6L, 4L);
        when(claimMapper.selectCount(any())).thenReturn(8L);
        CategoryStatRow row = new CategoryStatRow();
        row.setCategoryId(1L);
        row.setCategoryName("证件");
        row.setItemCount(6L);
        row.setCompletedCount(4L);
        when(itemMapper.categoryStats()).thenReturn(List.of(row));
        DailyCountRow daily = new DailyCountRow();
        daily.setDay(LocalDate.now());
        daily.setTotal(2L);
        when(itemMapper.dailyCounts(any())).thenReturn(List.of(daily));

        StatsVO stats = adminService.stats(7);

        assertThat(stats.totalItems()).isEqualTo(10);
        assertThat(stats.pendingAudit()).isEqualTo(3);
        assertThat(stats.completedClaims()).isEqualTo(8);
        assertThat(stats.returnRate()).isEqualTo(66.7);
        assertThat(stats.categoryStats().get(0).returnRate()).isEqualTo(66.7);
        assertThat(stats.trend()).hasSize(7);
        assertThat(stats.trend().get(6).count()).isEqualTo(2);
    }

    // ---------------- 工具 ----------------

    private LostItem item(int auditStatus) {
        LostItem item = new LostItem();
        item.setId(101L);
        item.setUserId(12L);
        item.setTitle("黑色钱包");
        item.setAuditStatus(auditStatus);
        item.setItemStatus(0);
        return item;
    }

    private AuditDTO dto(int status, String reason) {
        AuditDTO dto = new AuditDTO();
        dto.setAuditStatus(status);
        dto.setRejectReason(reason);
        return dto;
    }

    private CategoryDTO categoryDTO(String name) {
        CategoryDTO dto = new CategoryDTO();
        dto.setName(name);
        dto.setSort(1);
        return dto;
    }

    private User user(Long id, int role, int status) {
        User user = new User();
        user.setId(id);
        user.setStudentNo("2022304512");
        user.setNickname("小李同学");
        user.setRole(role);
        user.setStatus(status);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
