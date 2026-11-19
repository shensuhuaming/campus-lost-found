package com.campus.lostfound.security;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.UserStatusEnum;
import com.campus.lostfound.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 鉴权拦截器（详细设计 6.3、8.1）。
 * <p>
 * 校验签名与有效期 → 查库确认账号存在且未被禁用（禁用即时生效）→ 写入 ThreadLocal。
 * 公开接口（登录、注册、前台浏览等）直接放行。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** 无需登录的 GET 接口（前台浏览）。 */
    private static final List<String> PUBLIC_GET = List.of(
            "/api/items", "/api/items/**", "/api/categories", "/api/system/**", "/uploads/**");

    /** 无需登录的 POST 接口。 */
    private static final List<String> PUBLIC_POST = List.of(
            "/api/auth/login", "/api/auth/register");

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(JwtUtil jwtUtil, UserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method) || isPublic(method, request.getRequestURI())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(response, ResultCode.UNAUTHORIZED);
        }

        Long uid;
        try {
            Claims claims = jwtUtil.parse(header.substring(7));
            Object uidClaim = claims.get("uid");
            uid = uidClaim == null ? null : Long.valueOf(uidClaim.toString());
        } catch (Exception e) {
            return reject(response, ResultCode.UNAUTHORIZED);   // 签名错误、过期、格式非法
        }
        if (uid == null) {
            return reject(response, ResultCode.UNAUTHORIZED);
        }

        User user = userMapper.selectById(uid);
        if (user == null) {
            return reject(response, ResultCode.UNAUTHORIZED);
        }
        if (UserStatusEnum.DISABLED.getCode() == user.getStatus()) {
            return reject(response, ResultCode.USER_DISABLED);  // 1004，禁用即时生效
        }

        SecurityUtil.set(new LoginUser(user.getId(), user.getRole()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityUtil.clear();   // 防止线程复用串号
    }

    private boolean isPublic(String method, String uri) {
        if ("GET".equalsIgnoreCase(method)) {
            return PUBLIC_GET.stream().anyMatch(p -> MATCHER.match(p, uri));
        }
        if ("POST".equalsIgnoreCase(method)) {
            return PUBLIC_POST.contains(uri);
        }
        return false;
    }

    private boolean reject(HttpServletResponse response, ResultCode rc) throws IOException {
        response.setStatus(rc.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(rc)));
        return false;
    }
}
