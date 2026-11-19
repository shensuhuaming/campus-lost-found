package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.ClaimAdminRow;
import com.campus.lostfound.dto.ClaimCreateDTO;
import com.campus.lostfound.dto.ClaimQuery;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.LoginUser;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.vo.ClaimAdminVO;
import com.campus.lostfound.vo.ClaimResultVO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** U4.2~U4.5 单元测试（详细设计 4.4 与表 4-7）。 */
@ExtendWith(MockitoExtension.class)
class ClaimServiceImplTest {

    static {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> clazz : new Class<?>[]{Claim.class, LostItem.class, User.class, Message.class}) {
            TableInfoHelper.initTableInfo(assistant, clazz);
        }
    }

    @Mock
    private ClaimMapper claimMapper;
    @Mock
    private LostItemMapper itemMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MessageService messageService;
    @InjectMocks
    private ClaimServiceImpl claimService;

    @BeforeEach
    void setUp() {
        SecurityUtil.set(new LoginUser(13L, 0));      // 申请人：学生
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    // ---------------- U4.2 发起认领 ----------------

    @Test
    @DisplayName("发起认领：物品不存在或未过审返回 2004")
    void create_itemNotFound_returns2004() {
        when(itemMapper.selectById(101L)).thenReturn(null);

        assertThatThrownBy(() -> claimService.create(createDTO()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("发起认领：认领自己发布的信息返回 3001")
    void create_ownItem_returns3001() {
        LostItem item = item(101L, 13L, 1, 0);
        when(itemMapper.selectById(101L)).thenReturn(item);

        assertThatThrownBy(() -> claimService.create(createDTO()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CLAIM_SELF_NOT_ALLOWED);
    }

    @Test
    @DisplayName("发起认领：物品已完成返回 2003")
    void create_completedItem_returns2003() {
        LostItem item = item(101L, 12L, 1, 2);
        when(itemMapper.selectById(101L)).thenReturn(item);

        assertThatThrownBy(() -> claimService.create(createDTO()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_STATUS_ILLEGAL);
    }

    @Test
    @DisplayName("发起认领：寻物信息不可认领返回 2003")
    void create_lostItem_returns2003() {
        LostItem item = item(101L, 12L, 0, 0);
        when(itemMapper.selectById(101L)).thenReturn(item);

        assertThatThrownBy(() -> claimService.create(createDTO()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_STATUS_ILLEGAL);
    }

    @Test
    @DisplayName("发起认领：重复申请返回 3003")
    void create_duplicate_returns3003() {
        LostItem item = item(101L, 12L, 1, 0);
        when(itemMapper.selectById(101L)).thenReturn(item);
        when(claimMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> claimService.create(createDTO()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CLAIM_EXISTS);
        verify(claimMapper, never()).insert(any(Claim.class));
    }

    @Test
    @DisplayName("发起认领成功：插入申请、物品 0→1、通知发布者，返回最新状态")
    void create_success() {
        LostItem item = item(101L, 12L, 1, 0);
        when(itemMapper.selectById(101L)).thenReturn(item);
        when(claimMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(claimMapper.insert(any(Claim.class))).thenAnswer(inv -> {
            ((Claim) inv.getArgument(0)).setId(55L);
            return 1;
        });
        when(itemMapper.update(any(), any())).thenReturn(1);
        when(userMapper.selectById(13L)).thenReturn(applicant());

        ClaimResultVO result = claimService.create(createDTO());

        assertThat(result.claimId()).isEqualTo(55L);
        assertThat(result.itemStatus()).isEqualTo(1);
        verify(messageService).push(eq(12L), eq(MessageTypeEnum.CLAIM_APPLY), any(), eq(101L));
    }

    // ---------------- U4.3 核验通过 ----------------

    @Test
    @DisplayName("核验通过：非管理员返回 2002")
    void approve_nonAdmin_returns2002() {
        assertThatThrownBy(() -> claimService.approve(55L, "ok"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NO_PERMISSION);
    }

    @Test
    @DisplayName("核验通过：申请已处理返回 3002")
    void approve_notPending_returns3002() {
        SecurityUtil.set(new LoginUser(1L, 1));
        when(claimMapper.selectById(55L)).thenReturn(claim(1, 101L, 13L));

        assertThatThrownBy(() -> claimService.approve(55L, "ok"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.CLAIM_STATUS_ILLEGAL);
    }

    @Test
    @DisplayName("核验通过：物品已不在处理中被并发抢先，返回 2003（整体回滚）")
    void approve_itemNotProcessing_returns2003() {
        SecurityUtil.set(new LoginUser(1L, 1));
        when(claimMapper.selectById(55L)).thenReturn(claim(0, 101L, 13L));
        when(itemMapper.selectById(101L)).thenReturn(item(101L, 12L, 1, 2));
        when(itemMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> claimService.approve(55L, "ok"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_STATUS_ILLEGAL);
        verify(claimMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("核验通过成功：物品置已完成、其余待核验自动拒绝并通知")
    void approve_success_rejectsOthers() {
        SecurityUtil.set(new LoginUser(1L, 1));
        when(claimMapper.selectById(55L)).thenReturn(claim(0, 101L, 13L));
        LostItem item = item(101L, 12L, 1, 1);
        when(itemMapper.selectById(101L)).thenReturn(item);
        when(itemMapper.update(any(), any())).thenReturn(1);
        when(claimMapper.update(any(), any())).thenReturn(1);
        when(claimMapper.selectList(any())).thenReturn(List.of(claim(0, 101L, 77L)));

        ClaimResultVO result = claimService.approve(55L, "请到服务台领取");

        assertThat(result.itemStatus()).isEqualTo(2);
        verify(messageService).push(eq(77L), eq(MessageTypeEnum.CLAIM_RESULT), any(), eq(101L));
        verify(messageService).push(eq(13L), eq(MessageTypeEnum.CLAIM_RESULT), any(), eq(101L));
        verify(messageService).push(eq(12L), eq(MessageTypeEnum.CLAIM_RESULT), any(), eq(101L));
    }

    // ---------------- U4.4 核验拒绝 ----------------

    @Test
    @DisplayName("核验拒绝：未填原因返回 1001")
    void reject_blankReason_returns1001() {
        SecurityUtil.set(new LoginUser(1L, 1));

        assertThatThrownBy(() -> claimService.reject(55L, "  "))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR);
    }

    @Test
    @DisplayName("核验拒绝：没有其它待核验申请时物品退回进行中")
    void reject_lastPending_returnsItemToOngoing() {
        SecurityUtil.set(new LoginUser(1L, 1));
        when(claimMapper.selectById(55L)).thenReturn(claim(0, 101L, 13L));
        when(claimMapper.update(any(), any())).thenReturn(1);
        when(claimMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(itemMapper.update(any(), any())).thenReturn(1);

        ClaimResultVO result = claimService.reject(55L, "描述与物品不符");

        assertThat(result.itemStatus()).isZero();
        verify(itemMapper).update(any(), any());
        verify(messageService).push(eq(13L), eq(MessageTypeEnum.CLAIM_RESULT), any(), eq(101L));
    }

    @Test
    @DisplayName("核验拒绝：仍有其它待核验申请时物品保持处理中")
    void reject_withOthers_keepsProcessing() {
        SecurityUtil.set(new LoginUser(1L, 1));
        when(claimMapper.selectById(55L)).thenReturn(claim(0, 101L, 13L));
        when(claimMapper.update(any(), any())).thenReturn(1);
        when(claimMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        ClaimResultVO result = claimService.reject(55L, "描述与物品不符");

        assertThat(result.itemStatus()).isEqualTo(1);
        verify(itemMapper, never()).update(any(), any());
    }

    // ---------------- U4.5 核验列表 ----------------

    @Test
    @DisplayName("核验列表：非管理员返回 2002")
    void page_nonAdmin_returns2002() {
        assertThatThrownBy(() -> claimService.page(new ClaimQuery()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NO_PERMISSION);
    }

    @Test
    @DisplayName("核验列表：默认查待核验并按行组装申请人信息")
    void page_returnsRows() {
        SecurityUtil.set(new LoginUser(1L, 1));
        ClaimAdminRow row = new ClaimAdminRow();
        row.setId(55L);
        row.setItemId(101L);
        row.setItemTitle("黑色钱包");
        row.setApplicantId(13L);
        row.setApplicantNickname("王同学");
        row.setApplicantStudentNo("2022304601");
        row.setDetailDesc("内有一卡通");
        row.setStatus(0);
        row.setCreatedAt(LocalDateTime.now());
        when(claimMapper.search(any(), eq(0), any())).thenAnswer(inv -> {
            IPage<ClaimAdminRow> page = inv.getArgument(0);
            page.setRecords(List.of(row));
            page.setTotal(1);
            return page;
        });

        PageResult<ClaimAdminVO> result = claimService.page(new ClaimQuery());

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records().get(0).applicant().nickname()).isEqualTo("王同学");
        assertThat(result.records().get(0).statusText()).isEqualTo("待核验");
    }

    // ---------------- 工具 ----------------

    private ClaimCreateDTO createDTO() {
        ClaimCreateDTO dto = new ClaimCreateDTO();
        dto.setItemId(101L);
        dto.setDetailDesc("内有一卡通，姓名与我一致，可提供学号核验");
        return dto;
    }

    private LostItem item(Long id, Long userId, int type, int itemStatus) {
        LostItem item = new LostItem();
        item.setId(id);
        item.setUserId(userId);
        item.setType(type);
        item.setTitle("黑色钱包");
        item.setAuditStatus(1);
        item.setItemStatus(itemStatus);
        return item;
    }

    private Claim claim(int status, Long itemId, Long applicantId) {
        Claim claim = new Claim();
        claim.setId(55L);
        claim.setItemId(itemId);
        claim.setApplicantId(applicantId);
        claim.setStatus(status);
        claim.setDetailDesc("内有一卡通");
        claim.setCreatedAt(LocalDateTime.now());
        return claim;
    }

    private User applicant() {
        User user = new User();
        user.setId(13L);
        user.setNickname("王同学");
        user.setStudentNo("2022304601");
        return user;
    }
}
