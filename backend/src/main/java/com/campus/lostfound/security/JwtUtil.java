package com.campus.lostfound.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 签发与解析（HS256，详细设计 8.1）。
 * <p>
 * 载荷只放 {@code uid} 与 {@code role}，不放昵称等可变信息，避免令牌与库中数据不一致。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final Duration expire;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-hours:12}") long expireHours) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret 长度必须不少于 32 字节（HS256 要求）");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expire = Duration.ofHours(expireHours);
    }

    /** 签发令牌：claims = {uid, role}。 */
    public String generate(Long uid, Integer role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(uid))
                .claim("uid", uid)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expire)))
                .signWith(key)
                .compact();
    }

    /** 解析并校验签名与有效期，失败抛 {@link io.jsonwebtoken.JwtException}。 */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getExpiresInSeconds() {
        return expire.toSeconds();
    }
}
