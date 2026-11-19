package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.LoginDTO;
import com.campus.lostfound.dto.RegisterDTO;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.JwtUtil;
import com.campus.lostfound.security.LoginUser;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.vo.LoginVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * U1.1/U1.2/U1.3 单元测试（对应详细设计表 4-7）。
 * <p>
 * 全部 mock Mapper 与加密器，不连数据库；断言业务结果（错误码、落库字段、返回体），不断言实现细节。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterDTO registerDTO;

    @BeforeEach
    void setUp() {
        registerDTO = new RegisterDTO();
        registerDTO.setStudentNo("2022304512");
        registerDTO.setNickname("小李同学");
        registerDTO.setPhone("13800006621");
        registerDTO.setPassword("abc123456");
        registerDTO.setConfirmPassword("abc123456");
        SecurityUtil.set(new LoginUser(1L, 0));
    }

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    // ---------------- U1.1 注册 ----------------

    @Test
    @DisplayName("注册成功：密码落库为 BCrypt 摘要，角色=学生、状态=正常，返回新用户 id")
    void register_success_hashesPasswordAndSetsDefaults() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("abc123456")).thenReturn("$2a$10$hashed");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(12L);
            return 1;
        });

        Long id = authService.register(registerDTO);

        assertThat(id).isEqualTo(12L);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(saved.getPassword()).isNotEqualTo("abc123456");
        assertThat(saved.getRole()).isZero();
        assertThat(saved.getStatus()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("注册失败：学号已存在返回 1002，且不写库")
    void register_duplicateStudentNo_returns1002() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.USER_EXISTS);
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    @DisplayName("注册失败：并发插入触发唯一索引冲突时同样返回 1002")
    void register_duplicateKeyException_returns1002() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("uk_user_student_no"));

        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.USER_EXISTS);
    }

    @Test
    @DisplayName("注册失败：两次密码不一致返回 1001，且不查库")
    void register_passwordMismatch_returns1001() {
        registerDTO.setConfirmPassword("abc123457");

        assertThatThrownBy(() -> authService.register(registerDTO))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR);
        verify(userMapper, never()).selectCount(any(Wrapper.class));
    }

    // ---------------- U1.2 登录 ----------------

    @Test
    @DisplayName("登录成功：返回 token 与用户信息，响应中不含密码")
    void login_success_returnsTokenWithoutPassword() {
        User user = buildUser(12L, 0);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("abc123456", user.getPassword())).thenReturn(true);
        when(jwtUtil.generate(12L, 0)).thenReturn("jwt-token");
        when(jwtUtil.getExpiresInSeconds()).thenReturn(43200L);

        LoginVO vo = authService.login(loginDTO());

        assertThat(vo.token()).isEqualTo("jwt-token");
        assertThat(vo.tokenType()).isEqualTo("Bearer");
        assertThat(vo.expiresIn()).isEqualTo(43200L);
        assertThat(vo.user().id()).isEqualTo(12L);
        assertThat(vo.user().studentNo()).isEqualTo("2022304512");
    }

    @Test
    @DisplayName("登录失败：账号不存在返回 1003（与密码错误同码，防账号枚举）")
    void login_userNotFound_returns1003() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> authService.login(loginDTO()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.LOGIN_FAILED);
        verify(jwtUtil, never()).generate(any(), any());
    }

    @Test
    @DisplayName("登录失败：密码错误返回 1003，且不签发令牌")
    void login_wrongPassword_returns1003() {
        User user = buildUser(12L, 0);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("abc123456", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginDTO()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.LOGIN_FAILED);
        verify(jwtUtil, never()).generate(any(), any());
    }

    @Test
    @DisplayName("登录失败：账号被禁用返回 1004")
    void login_disabledUser_returns1004() {
        User user = buildUser(12L, 1);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("abc123456", user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginDTO()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.USER_DISABLED);
        verify(jwtUtil, never()).generate(any(), any());
    }

    // ---------------- U1.3 退出 ----------------

    @Test
    @DisplayName("退出登录：无状态令牌下只记录日志，不产生任何写库操作")
    void logout_doesNotTouchDatabase() {
        authService.logout();

        verify(userMapper, never()).insert(any(User.class));
        verify(userMapper, never()).updateById(any(User.class));
    }

    private LoginDTO loginDTO() {
        LoginDTO dto = new LoginDTO();
        dto.setStudentNo("2022304512");
        dto.setPassword("abc123456");
        return dto;
    }

    private User buildUser(Long id, int status) {
        User user = new User();
        user.setId(id);
        user.setStudentNo("2022304512");
        user.setNickname("小李同学");
        user.setPassword("$2a$10$hashed");
        user.setRole(0);
        user.setStatus(status);
        return user;
    }
}
