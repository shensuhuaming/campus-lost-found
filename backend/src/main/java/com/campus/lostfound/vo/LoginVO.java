package com.campus.lostfound.vo;

import com.campus.lostfound.entity.User;

/**
 * 登录响应（对齐 API 规格 2.2）。
 *
 * @param token     JWT
 * @param tokenType 固定 Bearer
 * @param expiresIn 有效期（秒），12 小时 = 43200
 * @param user      用户信息，不含密码
 */
public record LoginVO(String token, String tokenType, long expiresIn, UserVO user) {

    public static LoginVO of(String token, long expiresInSeconds, User user) {
        return new LoginVO(token, "Bearer", expiresInSeconds, UserVO.of(user));
    }
}
