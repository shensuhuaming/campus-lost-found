package com.campus.lostfound.security;

import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JWT 拦截器（详细设计 6.3、8.1）：
 * 未带令牌 401、令牌有效放行、账号被禁用即时拒绝、公开接口放行。
 */
class JwtInterceptorTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 12);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final JwtInterceptor interceptor = new JwtInterceptor(jwtUtil, userMapper, new ObjectMapper());

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
    }

    @Test
    @DisplayName("未携带令牌访问受保护接口：401 + code=2001，且不放行")
    void noToken_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean pass = interceptor.preHandle(request, response, new Object());

        assertThat(pass).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":2001");
    }

    @Test
    @DisplayName("令牌格式错误：401（不抛异常到上层）")
    void malformedToken_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("令牌有效且账号正常：放行，并把当前用户写入 ThreadLocal")
    void validToken_setsCurrentUser() throws Exception {
        User user = user(12L, 0);
        when(userMapper.selectById(12L)).thenReturn(user);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer " + jwtUtil.generate(12L, 0));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(SecurityUtil.getUserId()).isEqualTo(12L);

        interceptor.afterCompletion(request, response, new Object(), null);
        assertThat(SecurityUtil.get()).isNull();   // 请求结束清除，避免线程复用串号
    }

    @Test
    @DisplayName("账号被禁用：403 + code=1004（禁用即时生效）")
    void disabledUser_returns1004() throws Exception {
        when(userMapper.selectById(12L)).thenReturn(user(12L, 1));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer " + jwtUtil.generate(12L, 0));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":1004");
    }

    @Test
    @DisplayName("账号已被删除：401")
    void userNotFound_returns401() throws Exception {
        when(userMapper.selectById(any())).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer " + jwtUtil.generate(12L, 0));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("公开接口：无令牌也放行（列表、详情、分类、登录注册）")
    void publicEndpoints_passWithoutToken() throws Exception {
        assertThat(interceptor.preHandle(new MockHttpServletRequest("GET", "/api/items"), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(new MockHttpServletRequest("GET", "/api/items/101"), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(new MockHttpServletRequest("GET", "/api/categories"), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(new MockHttpServletRequest("POST", "/api/auth/login"), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(new MockHttpServletRequest("POST", "/api/auth/register"), new MockHttpServletResponse(), new Object())).isTrue();
    }

    private User user(Long id, int status) {
        User u = new User();
        u.setId(id);
        u.setStudentNo("2022304512");
        u.setNickname("小李同学");
        u.setPassword("$2a$10$hashed");
        u.setRole(0);
        u.setStatus(status);
        return u;
    }
}
