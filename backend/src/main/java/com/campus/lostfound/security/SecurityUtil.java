package com.campus.lostfound.security;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.enums.RoleEnum;

/**
 * 当前登录用户上下文。
 * <p>
 * 由 {@link JwtInterceptor} 在请求进入时写入、请求结束时清除；
 * 业务层只从这里取 uid，**不接受前端传入的 userId**（详细设计 3.6）。
 */
public final class SecurityUtil {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private SecurityUtil() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user.id();
    }

    public static Integer getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.role();
    }

    public static boolean isAdmin() {
        Integer role = getRole();
        return role != null && role == RoleEnum.ADMIN.getCode();
    }

    /** 非管理员直接抛 2002。 */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
    }
}
