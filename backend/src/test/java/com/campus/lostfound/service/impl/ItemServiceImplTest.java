package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.ItemQuery;
import com.campus.lostfound.dto.ItemSaveDTO;
import com.campus.lostfound.entity.Category;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.Comment;
import com.campus.lostfound.entity.ItemImage;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.CommentMapper;
import com.campus.lostfound.mapper.ItemImageMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.LoginUser;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.vo.ItemCardVO;
import com.campus.lostfound.vo.ItemDetailVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** U2.1~U2.5、U3.1~U3.3 单元测试（详细设计 4.2、4.3 与表 4-7）。 */
@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    static {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> clazz : new Class<?>[]{LostItem.class, ItemImage.class, Claim.class,
                Comment.class, Category.class, User.class, Message.class}) {
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
    private CommentMapper commentMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MessageService messageService;
    @InjectMocks
    private ItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        SecurityUtil.set(new LoginUser(12L, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    // ---------------- U2.1 发布 ----------------

    @Test
    @DisplayName("发布成功：进入待审核、进行中，归属当前用户")
    void create_success() {
        when(categoryMapper.selectById(1L)).thenReturn(category());
        when(itemMapper.insert(any(LostItem.class))).thenAnswer(inv -> {
            ((LostItem) inv.getArgument(0)).setId(101L);
            return 1;
        });

        Long id = itemService.create(dto(1, List.of()));

        assertThat(id).isEqualTo(101L);
        org.mockito.ArgumentCaptor<LostItem> captor = org.mockito.ArgumentCaptor.forClass(LostItem.class);
        verify(itemMapper).insert(captor.capture());
        assertThat(captor.getValue().getAuditStatus()).isZero();
        assertThat(captor.getValue().getItemStatus()).isZero();
        assertThat(captor.getValue().getUserId()).isEqualTo(12L);
    }

    @Test
    @DisplayName("发布失败：分类不存在返回 5003")
    void create_categoryMissing_returns5003() {
        when(categoryMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> itemService.create(dto(1, List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CATEGORY_NOT_FOUND);
    }

    @Test
    @DisplayName("发布失败：超过 3 张图返回 4004")
    void create_tooManyImages_returns4004() {
        when(categoryMapper.selectById(1L)).thenReturn(category());
        List<String> urls = List.of(
                "/uploads/202611/" + "a".repeat(32) + ".png",
                "/uploads/202611/" + "b".repeat(32) + ".png",
                "/uploads/202611/" + "c".repeat(32) + ".png",
                "/uploads/202611/" + "d".repeat(32) + ".png");

        assertThatThrownBy(() -> itemService.create(dto(1, urls)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.IMAGE_TOO_MANY);
    }

    @Test
    @DisplayName("发布失败：图片地址不符合白名单（外部链接/路径穿越）返回 1001")
    void create_illegalImageUrl_returns1001() {
        when(categoryMapper.selectById(1L)).thenReturn(category());
        assertThatThrownBy(() -> itemService.create(dto(1, List.of("https://evil.com/a.png"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR);
    }

    @Test
    @DisplayName("发布失败：图片路径合法但文件不存在返回 1001")
    void create_imageNotExists_returns1001() {
        when(categoryMapper.selectById(1L)).thenReturn(category());
        String url = "/uploads/202611/" + "f".repeat(32) + ".png";

        assertThatThrownBy(() -> itemService.create(dto(1, List.of(url))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR);
    }

    // ---------------- U2.3 编辑 ----------------

    @Test
    @DisplayName("编辑失败：非本人且非管理员返回 2002")
    void update_notOwner_returns2002() {
        when(itemMapper.selectById(101L)).thenReturn(item(101L, 12L, 1, 0));

        SecurityUtil.set(new LoginUser(99L, 0));
        assertThatThrownBy(() -> itemService.update(101L, dto(1, List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NO_PERMISSION);
    }

    @Test
    @DisplayName("编辑失败：处理中的信息不可编辑（防止与在途认领冲突）返回 2003")
    void update_processing_returns2003() {
        when(itemMapper.selectById(101L)).thenReturn(item(101L, 12L, 1, 1));

        assertThatThrownBy(() -> itemService.update(101L, dto(1, List.of())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_STATUS_ILLEGAL);
    }

    @Test
    @DisplayName("编辑成功：审核状态重置为待审核并清空驳回理由")
    void update_resetsAudit() {
        LostItem item = item(101L, 12L, 1, 0);
        item.setAuditStatus(1);
        item.setRejectReason("旧理由");
        when(itemMapper.selectById(101L)).thenReturn(item);
        when(categoryMapper.selectById(1L)).thenReturn(category());

        itemService.update(101L, dto(1, List.of()));

        assertThat(item.getAuditStatus()).isZero();
        assertThat(item.getRejectReason()).isNull();
        verify(itemMapper).updateById(item);
    }

    // ---------------- U2.4 关闭 ----------------

    @Test
    @DisplayName("关闭成功：发布者关闭进行中信息，状态变 3 并通知发布者")
    void close_owner_success() {
        when(itemMapper.selectById(101L)).thenReturn(item(101L, 12L, 1, 0));
        when(itemMapper.update(any(), any())).thenReturn(1);
        when(claimMapper.selectList(any())).thenReturn(List.of());

        itemService.close(101L, null);

        verify(messageService).push(eq(12L), eq(MessageTypeEnum.SYSTEM), any(), eq(101L));
    }

    @Test
    @DisplayName("关闭失败：管理员未填理由返回 1001")
    void close_adminWithoutReason_returns1001() {
        when(itemMapper.selectById(101L)).thenReturn(item(101L, 12L, 1, 0));
        SecurityUtil.set(new LoginUser(1L, 1));

        assertThatThrownBy(() -> itemService.close(101L, "  "))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR);
    }

    @Test
    @DisplayName("关闭失败：已完成的信息不可关闭返回 2003")
    void close_completed_returns2003() {
        when(itemMapper.selectById(101L)).thenReturn(item(101L, 12L, 1, 2));

        assertThatThrownBy(() -> itemService.close(101L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_STATUS_ILLEGAL);
    }

    @Test
    @DisplayName("关闭时：待核验认领被自动拒绝并通知申请人")
    void close_rejectsPendingClaims() {
        when(itemMapper.selectById(101L)).thenReturn(item(101L, 12L, 1, 1));
        when(itemMapper.update(any(), any())).thenReturn(1);
        Claim claim = new Claim();
        claim.setId(55L);
        claim.setApplicantId(77L);
        when(claimMapper.selectList(any())).thenReturn(List.of(claim));
        when(claimMapper.update(any(), any())).thenReturn(1);

        itemService.close(101L, null);

        verify(messageService).push(eq(77L), eq(MessageTypeEnum.CLAIM_RESULT), any(), eq(101L));
    }

    // ---------------- U2.5 寻物推进 ----------------

    @Test
    @DisplayName("寻物推进成功：进行中 → 处理中")
    void advance_lost_success() {
        LostItem lost = item(101L, 12L, 0, 0);
        lost.setAuditStatus(1);
        when(itemMapper.selectById(101L)).thenReturn(lost);
        when(itemMapper.update(any(), any())).thenReturn(1);

        itemService.advance(101L, 1);

        verify(itemMapper).update(any(), any());
        verify(messageService).push(eq(12L), eq(MessageTypeEnum.SYSTEM), any(), eq(101L));
    }

    @Test
    @DisplayName("寻物推进失败：拾物调用返回 2003")
    void advance_found_returns2003() {
        LostItem found = item(101L, 12L, 1, 0);
        found.setAuditStatus(1);
        when(itemMapper.selectById(101L)).thenReturn(found);

        assertThatThrownBy(() -> itemService.advance(101L, 1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_STATUS_ILLEGAL);
    }

    @Test
    @DisplayName("寻物推进失败：跳级（进行中 → 已完成）返回 2003")
    void advance_skipLevel_returns2003() {
        LostItem lost = item(101L, 12L, 0, 0);
        lost.setAuditStatus(1);
        when(itemMapper.selectById(101L)).thenReturn(lost);

        assertThatThrownBy(() -> itemService.advance(101L, 2))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_STATUS_ILLEGAL);
    }

    // ---------------- U3.1 / U3.3 ----------------

    @Test
    @DisplayName("列表：空结果返回空分页")
    void page_empty() {
        when(itemMapper.selectPage(any(), any())).thenAnswer(inv -> {
            IPage<LostItem> page = inv.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        PageResult<ItemCardVO> result = itemService.page(new ItemQuery());

        assertThat(result.total()).isZero();
        assertThat(result.records()).isEmpty();
    }

    @Test
    @DisplayName("列表：关键词走搜索 SQL 并按页返回卡片")
    void page_withKeyword() {
        LostItem record = item(101L, 12L, 1, 1);
        record.setAuditStatus(1);
        when(itemMapper.search(any(), any(), any(), any())).thenAnswer(inv -> {
            IPage<LostItem> page = inv.getArgument(0);
            page.setRecords(List.of(record));
            page.setTotal(1);
            return page;
        });
        when(categoryMapper.selectBatchIds(any())).thenReturn(List.of(category()));
        when(imageMapper.selectList(any())).thenReturn(List.of());

        ItemQuery query = new ItemQuery();
        query.setKeyword("钱包");
        PageResult<ItemCardVO> result = itemService.page(query);

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).categoryName()).isEqualTo("证件");
        assertThat(result.records().get(0).itemStatusText()).isEqualTo("处理中");
    }

    @Test
    @DisplayName("详情：非本人非管理员看到掩码手机号，并带出我的认领状态")
    void detail_masksContactAndFillsClaimStatus() {
        LostItem item = item(101L, 12L, 1, 1);
        item.setAuditStatus(1);
        item.setContact("13800006621");
        when(itemMapper.selectById(101L)).thenReturn(item);
        when(categoryMapper.selectById(1L)).thenReturn(category());
        when(userMapper.selectById(12L)).thenReturn(publisher());
        when(imageMapper.selectList(any())).thenReturn(List.of());
        when(commentMapper.selectList(any())).thenReturn(List.of());
        Claim claim = new Claim();
        claim.setStatus(0);
        when(claimMapper.selectOne(any())).thenReturn(claim);

        SecurityUtil.set(new LoginUser(99L, 0));
        ItemDetailVO vo = itemService.detail(101L);

        assertThat(vo.contact()).isEqualTo("138****6621");
        assertThat(vo.publisher().studentNo()).isEqualTo("2022****12");
        assertThat(vo.myClaimStatus()).isZero();
    }

    @Test
    @DisplayName("详情：未过审信息对他人不可见返回 2004")
    void detail_pendingInvisible_returns2004() {
        LostItem item = item(101L, 12L, 1, 0);
        item.setAuditStatus(0);
        when(itemMapper.selectById(101L)).thenReturn(item);

        SecurityUtil.set(new LoginUser(99L, 0));
        assertThatThrownBy(() -> itemService.detail(101L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("分类列表：按 sort 升序返回")
    void categories_ordered() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(category()));

        assertThat(itemService.categories()).hasSize(1);
        assertThat(itemService.categories().get(0).name()).isEqualTo("证件");
    }

    // ---------------- 工具 ----------------

    private ItemSaveDTO dto(int type, List<String> images) {
        ItemSaveDTO dto = new ItemSaveDTO();
        dto.setType(type);
        dto.setTitle("黑色钱包（含校园卡）");
        dto.setDescription("图书馆三楼捡到黑色钱包，内有一卡通与少量现金");
        dto.setCategoryId(1L);
        dto.setHappenTime(LocalDateTime.now().minusHours(2));
        dto.setHappenPlace("图书馆三楼");
        dto.setContact("13800006621");
        dto.setImageUrls(images);
        return dto;
    }

    private LostItem item(Long id, Long userId, int type, int itemStatus) {
        LostItem item = new LostItem();
        item.setId(id);
        item.setUserId(userId);
        item.setType(type);
        item.setTitle("黑色钱包（含校园卡）");
        item.setDescription("图书馆三楼捡到黑色钱包，内有一卡通与少量现金");
        item.setCategoryId(1L);
        item.setItemStatus(itemStatus);
        item.setAuditStatus(0);
        item.setContact("13800006621");
        item.setCreatedAt(LocalDateTime.now());
        return item;
    }

    private Category category() {
        Category category = new Category();
        category.setId(1L);
        category.setName("证件");
        category.setSort(1);
        return category;
    }

    private User publisher() {
        User user = new User();
        user.setId(12L);
        user.setStudentNo("2022304512");
        user.setNickname("小李同学");
        return user;
    }
}
