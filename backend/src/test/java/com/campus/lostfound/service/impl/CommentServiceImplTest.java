package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.CommentCreateDTO;
import com.campus.lostfound.entity.Comment;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.MessageTypeEnum;
import com.campus.lostfound.mapper.CommentMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.LoginUser;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.MessageService;
import com.campus.lostfound.vo.CommentCreatedVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** U5.1/U5.2 留言（详细设计 4.5.1、4.5.2）。 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    static {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> clazz : new Class<?>[]{Comment.class, LostItem.class, User.class, Message.class}) {
            TableInfoHelper.initTableInfo(assistant, clazz);
        }
    }

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private LostItemMapper itemMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MessageService messageService;
    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        SecurityUtil.set(new LoginUser(13L, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    @DisplayName("发表留言：物品不存在或未过审返回 2004")
    void create_itemNotFound_returns2004() {
        when(itemMapper.selectById(101L)).thenReturn(null);

        assertThatThrownBy(() -> commentService.create(dto()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("发表留言：成功落库并通知发布者（带留言人昵称）")
    void create_notifiesPublisher() {
        when(itemMapper.selectById(101L)).thenReturn(item(12L));
        when(commentMapper.insert(any(Comment.class))).thenAnswer(inv -> {
            ((Comment) inv.getArgument(0)).setId(301L);
            return 1;
        });
        when(userMapper.selectById(13L)).thenReturn(commenter());

        CommentCreatedVO vo = commentService.create(dto());

        assertThat(vo.id()).isEqualTo(301L);
        verify(messageService).push(eq(12L), eq(13L), eq(MessageTypeEnum.COMMENT), any(), eq(101L));
    }

    @Test
    @DisplayName("发表留言：给自己发布的信息留言不推送消息")
    void create_selfComment_noMessage() {
        when(itemMapper.selectById(101L)).thenReturn(item(13L));
        when(commentMapper.insert(any(Comment.class))).thenAnswer(inv -> {
            ((Comment) inv.getArgument(0)).setId(301L);
            return 1;
        });

        commentService.create(dto());

        verify(messageService, never()).push(any(), any(), any(), any());
    }

    @Test
    @DisplayName("删除留言：不存在返回 2004")
    void delete_notFound_returns2004() {
        when(commentMapper.selectById(301L)).thenReturn(null);

        assertThatThrownBy(() -> commentService.delete(301L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NOT_FOUND);
    }

    @Test
    @DisplayName("删除留言：非本人且非管理员返回 2002")
    void delete_notOwner_returns2002() {
        Comment comment = comment(77L);
        when(commentMapper.selectById(301L)).thenReturn(comment);

        assertThatThrownBy(() -> commentService.delete(301L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.NO_PERMISSION);
        verify(commentMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("删除留言：本人可删除")
    void delete_owner_success() {
        when(commentMapper.selectById(301L)).thenReturn(comment(13L));

        commentService.delete(301L);

        verify(commentMapper).deleteById(301L);
    }

    @Test
    @DisplayName("删除留言：管理员可删除他人留言")
    void delete_admin_success() {
        SecurityUtil.set(new LoginUser(1L, 1));
        when(commentMapper.selectById(301L)).thenReturn(comment(77L));

        commentService.delete(301L);

        verify(commentMapper).deleteById(301L);
    }

    private CommentCreateDTO dto() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setItemId(101L);
        dto.setContent("这个钱包好像是隔壁宿舍同学的，帮忙转告一下");
        return dto;
    }

    private LostItem item(Long ownerId) {
        LostItem item = new LostItem();
        item.setId(101L);
        item.setUserId(ownerId);
        item.setTitle("黑色钱包");
        item.setAuditStatus(1);
        return item;
    }

    private Comment comment(Long userId) {
        Comment comment = new Comment();
        comment.setId(301L);
        comment.setItemId(101L);
        comment.setUserId(userId);
        comment.setContent("留言");
        return comment;
    }

    private User commenter() {
        User user = new User();
        user.setId(13L);
        user.setNickname("王同学");
        return user;
    }
}
