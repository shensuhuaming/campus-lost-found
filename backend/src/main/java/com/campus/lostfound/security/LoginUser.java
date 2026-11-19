package com.campus.lostfound.security;

/** 拦截器解析 token 后放入 ThreadLocal 的当前用户信息。 */
public record LoginUser(Long id, Integer role) {
}
