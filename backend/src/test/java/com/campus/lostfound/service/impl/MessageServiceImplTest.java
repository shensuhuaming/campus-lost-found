package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.MessageQuery;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.MessageMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.LoginUser;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.vo.MessageVO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** U5.4 消息列表、已读与未读数（详细设计 4.5.4）。 */
@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    static {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> clazz : new Class<?>[]{Message.class, User.class}) {
            TableInfoHelper.initTableInfo(assistant, clazz);
        }
    }

    @Mock
    private MessageMapper messageMapper;
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private MessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
        SecurityUtil.set(new LoginUser(13L, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    @DisplayName("消息列表：系统消息显示'系统'，用户消息显示昵称")
    void page_mapsSenderNickname() {
        Message system = message(900L, 0L, 3);
        Message fromUser = message(901L, 77L, 2);
        when(messageMapper.selectPage(any(), any())).thenAnswer(inv -> {
            IPage<Message> page = inv.getArgument(0);
            page.setRecords(List.of(system, fromUser));
            page.setTotal(2);
            return page;
        });
        User sender = new User();
        sender.setId(77L);
        sender.setNickname("王同学");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(sender));

        PageResult<MessageVO> result = messageService.page(new MessageQuery());

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.records().get(0).fromNickname()).isEqualTo("系统");
        assertThat(result.records().get(1).fromNickname()).isEqualTo("王同学");
        assertThat(result.records().get(0).typeText()).isEqualTo("认领申请提醒");
    }

    @Test
    @DisplayName("消息列表：空结果返回空分页")
    void page_empty() {
        when(messageMapper.selectPage(any(), any())).thenAnswer(inv -> {
            IPage<Message> page = inv.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        assertThat(messageService.page(new MessageQuery()).records()).isEmpty();
    }

    @Test
    @DisplayName("标记已读：消息不存在返回 2004")
    void markRead_notFound_returns2004() {
        when(messageMapper.selectById(900L)).thenReturn(null);

        assertThatThrownBy(() -> messageService.markRead(900L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("标记已读：他人消息返回 2002")
    void markRead_notOwner_returns2002() {
        Message message = message(900L, 0L, 3);
        message.setToUserId(99L);
        when(messageMapper.selectById(900L)).thenReturn(message);

        assertThatThrownBy(() -> messageService.markRead(900L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NO_PERMISSION);
    }

    @Test
    @DisplayName("标记已读：成功更新 is_read=1")
    void markRead_success() {
        Message message = message(900L, 0L, 3);
        when(messageMapper.selectById(900L)).thenReturn(message);

        messageService.markRead(900L);

        assertThat(message.getIsRead()).isEqualTo(1);
        verify(messageMapper).updateById(message);
    }

    @Test
    @DisplayName("标记已读：已读重复调用幂等，不再更新")
    void markRead_idempotent() {
        Message message = message(900L, 0L, 3);
        message.setIsRead(1);
        when(messageMapper.selectById(900L)).thenReturn(message);

        messageService.markRead(900L);

        verify(messageMapper, never()).updateById(any(Message.class));
    }

    @Test
    @DisplayName("未读数：只统计本人未读")
    void unreadCount_countsOwnUnread() {
        when(messageMapper.selectCount(any(Wrapper.class))).thenReturn(3L);

        assertThat(messageService.unreadCount()).isEqualTo(3);
    }

    private Message message(Long id, Long fromUserId, int type) {
        Message message = new Message();
        message.setId(id);
        message.setToUserId(13L);
        message.setFromUserId(fromUserId);
        message.setType(type);
        message.setContent("测试消息");
        message.setIsRead(0);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}
