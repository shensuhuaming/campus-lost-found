package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageQuery;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.PasswordDTO;
import com.campus.lostfound.dto.ProfileDTO;
import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.Comment;
import com.campus.lostfound.entity.ItemImage;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.CommentMapper;
import com.campus.lostfound.mapper.ItemImageMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.LoginUser;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.vo.ClaimVO;
import com.campus.lostfound.vo.CommentVO;
import com.campus.lostfound.vo.ItemListVO;
import com.campus.lostfound.vo.ProfileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** U1.4~U1.6 单元测试（详细设计 4.1.4 ~ 4.1.6、表 4-7）。 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private LostItemMapper itemMapper;
    @Mock
    private ItemImageMapper imageMapper;
    @Mock
    private ClaimMapper claimMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;


    /** 纯 Mockito 测试没有 Spring 容器，需手动初始化 MyBatis-Plus 的字段缓存（lambda 条件要用）。 */
    static {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> clazz : new Class<?>[]{User.class, LostItem.class, Claim.class, Comment.class,
                ItemImage.class, Category.class}) {
            TableInfoHelper.initTableInfo(assistant, clazz);
        }
    }

    @BeforeEach
    void setUp() {
        SecurityUtil.set(new LoginUser(12L, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    // ---------------- U1.4 个人资料 ----------------

    @Test
    @DisplayName("查看资料：返回四项统计（发布/认领/待核验/留言）")
    void getProfile_returnsStats() {
        when(userMapper.selectById(12L)).thenReturn(user());
        when(itemMapper.selectCount(any(Wrapper.class))).thenReturn(6L);
        when(claimMapper.selectCount(any(Wrapper.class))).thenReturn(3L, 1L);
        when(commentMapper.selectCount(any(Wrapper.class))).thenReturn(8L);

        ProfileVO vo = userService.getProfile();

        assertThat(vo.id()).isEqualTo(12L);
        assertThat(vo.stat().published()).isEqualTo(6);
        assertThat(vo.stat().claiming()).isEqualTo(3);
        assertThat(vo.stat().claimingPending()).isEqualTo(1);
        assertThat(vo.stat().comments()).isEqualTo(8);
    }

    @Test
    @DisplayName("查看资料：用户不存在返回 2004")
    void getProfile_userNotFound_returns2004() {
        when(userMapper.selectById(12L)).thenReturn(null);

        assertThatThrownBy(() -> userService.getProfile())
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("修改资料：成功更新昵称与手机号，学号与角色不变")
    void updateProfile_success() {
        when(userMapper.selectById(12L)).thenReturn(user());

        ProfileDTO dto = new ProfileDTO();
        dto.setNickname("小李");
        dto.setPhone("13900001111");
        var vo = userService.updateProfile(dto);

        assertThat(vo.nickname()).isEqualTo("小李");
        assertThat(vo.phone()).isEqualTo("13900001111");
        assertThat(vo.studentNo()).isEqualTo("2022304512");
        assertThat(vo.role()).isZero();
    }

    // ---------------- U1.5 修改密码 ----------------

    @Test
    @DisplayName("修改密码：两次新密码不一致返回 1001")
    void changePassword_confirmMismatch_returns1001() {
        assertThatThrownBy(() -> userService.changePassword(password("abc123456", "xyz987654", "xyz987655")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR);
    }

    @Test
    @DisplayName("修改密码：新旧密码相同返回 1001")
    void changePassword_sameAsOld_returns1001() {
        assertThatThrownBy(() -> userService.changePassword(password("abc123456", "abc123456", "abc123456")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR);
    }

    @Test
    @DisplayName("修改密码：原密码错误返回 1005 且不更新")
    void changePassword_wrongOld_returns1005() {
        when(userMapper.selectById(12L)).thenReturn(user());
        when(passwordEncoder.matches("wrong123", "$2a$10$hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(password("wrong123", "xyz987654", "xyz987654")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.OLD_PASSWORD_ERROR);
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("修改密码：成功时以旧摘要为乐观条件更新")
    void changePassword_success() {
        when(userMapper.selectById(12L)).thenReturn(user());
        when(passwordEncoder.matches("abc123456", "$2a$10$hashed")).thenReturn(true);
        when(passwordEncoder.encode("xyz987654")).thenReturn("$2a$10$new");
        when(userMapper.update(any(), any())).thenReturn(1);

        userService.changePassword(password("abc123456", "xyz987654", "xyz987654"));

        verify(passwordEncoder).encode("xyz987654");
        verify(userMapper).update(any(), any());
    }

    @Test
    @DisplayName("修改密码：并发导致影响行数为 0 时返回 1005")
    void changePassword_concurrentConflict_returns1005() {
        when(userMapper.selectById(12L)).thenReturn(user());
        when(passwordEncoder.matches("abc123456", "$2a$10$hashed")).thenReturn(true);
        when(passwordEncoder.encode("xyz987654")).thenReturn("$2a$10$new");
        when(userMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> userService.changePassword(password("abc123456", "xyz987654", "xyz987654")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.OLD_PASSWORD_ERROR);
    }

    // ---------------- U1.6 我的三个列表 ----------------

    @Test
    @DisplayName("我的发布：分页 + 补齐分类名、封面图、认领数与状态文案")
    void myItems_enrichesFields() {
        LostItem item = item(101L, 1, "黑色钱包", 1L, 1, 1);
        when(itemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            IPage<LostItem> page = invocation.getArgument(0);
            page.setRecords(List.of(item));
            page.setTotal(1);
            return page;
        });
        when(categoryMapper.selectBatchIds(any())).thenReturn(List.of(category(1L, "证件")));
        when(imageMapper.selectList(any())).thenReturn(List.of(image(101L, "/uploads/202611/a1.png")));
        when(claimMapper.selectMaps(any())).thenReturn(List.of(Map.of("item_id", 101L, "cnt", 2L)));

        PageResult<ItemListVO> result = userService.myItems(new PageQuery(), null, null);

        assertThat(result.total()).isEqualTo(1);
        ItemListVO vo = result.records().get(0);
        assertThat(vo.categoryName()).isEqualTo("证件");
        assertThat(vo.coverImage()).isEqualTo("/uploads/202611/a1.png");
        assertThat(vo.claimCount()).isEqualTo(2);
        assertThat(vo.auditStatusText()).isEqualTo("已通过");
        assertThat(vo.itemStatusText()).isEqualTo("处理中");
    }

    @Test
    @DisplayName("我的发布：空结果返回空分页而不是 null")
    void myItems_emptyPage() {
        when(itemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            IPage<LostItem> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        PageResult<ItemListVO> result = userService.myItems(new PageQuery(), null, null);

        assertThat(result.total()).isZero();
        assertThat(result.records()).isEmpty();
    }

    @Test
    @DisplayName("我的认领：补齐物品标题、物品状态与申请状态文案")
    void myClaims_enrichesItemTitle() {
        when(claimMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            IPage<Claim> page = invocation.getArgument(0);
            page.setRecords(List.of(claim(55L, 101L, 1)));
            page.setTotal(1);
            return page;
        });
        when(itemMapper.selectBatchIds(any())).thenReturn(List.of(item(101L, 1, "黑色钱包", 1L, 1, 2)));

        PageResult<ClaimVO> result = userService.myClaims(new PageQuery(), null);

        ClaimVO vo = result.records().get(0);
        assertThat(vo.itemTitle()).isEqualTo("黑色钱包");
        assertThat(vo.itemStatus()).isEqualTo(2);
        assertThat(vo.statusText()).isEqualTo("已通过");
    }

    @Test
    @DisplayName("我的留言：补齐物品标题")
    void myComments_enrichesItemTitle() {
        Comment comment = new Comment();
        comment.setId(301L);
        comment.setItemId(101L);
        comment.setUserId(12L);
        comment.setContent("已联系失主，谢谢楼主");
        comment.setCreatedAt(LocalDateTime.now());
        when(commentMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            IPage<Comment> page = invocation.getArgument(0);
            page.setRecords(List.of(comment));
            page.setTotal(1);
            return page;
        });
        when(itemMapper.selectBatchIds(any())).thenReturn(List.of(item(101L, 1, "黑色钱包", 1L, 1, 1)));

        PageResult<CommentVO> result = userService.myComments(new PageQuery());

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).itemTitle()).isEqualTo("黑色钱包");
    }

    @Test
    @DisplayName("分页参数越界：page=0 纠正为 1，size=100 截断为 50")
    void pageQuery_normalizes() {
        PageQuery q = new PageQuery();
        q.setPage(0);
        q.setSize(100);
        assertThat(q.safePage()).isEqualTo(1);
        assertThat(q.safeSize()).isEqualTo(50);
    }

    // ---------------- 工具 ----------------

    private User user() {
        User u = new User();
        u.setId(12L);
        u.setStudentNo("2022304512");
        u.setNickname("小李同学");
        u.setPhone("13800006621");
        u.setPassword("$2a$10$hashed");
        u.setRole(0);
        u.setStatus(0);
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }

    private PasswordDTO password(String oldPwd, String newPwd, String confirm) {
        PasswordDTO dto = new PasswordDTO();
        dto.setOldPassword(oldPwd);
        dto.setNewPassword(newPwd);
        dto.setConfirmPassword(confirm);
        return dto;
    }

    private LostItem item(Long id, int type, String title, Long categoryId, int auditStatus, int itemStatus) {
        LostItem i = new LostItem();
        i.setId(id);
        i.setType(type);
        i.setTitle(title);
        i.setCategoryId(categoryId);
        i.setAuditStatus(auditStatus);
        i.setItemStatus(itemStatus);
        i.setUserId(12L);
        i.setCreatedAt(LocalDateTime.now());
        return i;
    }

    private Category category(Long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private ItemImage image(Long itemId, String url) {
        ItemImage img = new ItemImage();
        img.setItemId(itemId);
        img.setUrl(url);
        img.setSort(0);
        return img;
    }

    private Claim claim(Long id, Long itemId, int status) {
        Claim c = new Claim();
        c.setId(id);
        c.setItemId(itemId);
        c.setApplicantId(12L);
        c.setStatus(status);
        c.setDetailDesc("内有一卡通");
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }
}
