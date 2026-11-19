package com.campus.lostfound.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JWT 签发与解析（详细设计 8.1）。 */
class JwtUtilTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 12);

    @Test
    @DisplayName("签发后解析：uid 与 role 与签发时一致，有效期 12 小时")
    void generateAndParse_roundTrip() {
        String token = jwtUtil.generate(12L, 1);
        Claims claims = jwtUtil.parse(token);

        assertThat(claims.get("uid").toString()).isEqualTo("12");
        assertThat(claims.get("role").toString()).isEqualTo("1");
        assertThat(jwtUtil.getExpiresInSeconds()).isEqualTo(43200L);
    }

    @Test
    @DisplayName("篡改令牌：解析抛 JwtException（签名校验失败）")
    void parse_tamperedToken_throws() {
        String token = jwtUtil.generate(12L, 0);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtUtil.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("密钥过短：构造时直接拒绝，避免弱密钥上线")
    void constructor_shortSecret_throws() {
        assertThatThrownBy(() -> new JwtUtil("short", 12))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
