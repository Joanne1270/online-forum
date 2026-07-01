package com.forum.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public final class JwtUtil {

    public static final String SECRET = "online-forum-jwt-secret-key-2026-very-long";
    private static final long EXPIRE_MS = 7 * 24 * 60 * 60 * 1000L;

    private JwtUtil() {
    }

    private static SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(Long userId, String nickname, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("nickname", nickname, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(key())
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public static String getNickname(Claims claims) {
        String nickname = claims.get("nickname", String.class);
        if (nickname != null) {
            return nickname;
        }
        return claims.get("username", String.class);
    }

    public static String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
