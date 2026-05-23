package com.ccc.zerocodegenerateproject.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Autowired
    private JwtProperties jwtProperties;

    // 1. 统一获取密钥的方法
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    private static final long EXPIRE_TIME = 2 * 60 * 60 * 1000;

    // 2. 生成 Token
    public String createToken(Long id, String username) {
        Date expireDate = new Date(System.currentTimeMillis() + EXPIRE_TIME);
        return Jwts.builder()
                .setSubject(id.toString())
                .claim("id", id)
                .claim("username", username)
                .setExpiration(expireDate)
                .signWith(getSecretKey())
                .compact();
    }

    // 3. 解析 token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage()); // 加行日志，方便你在控制台看报错
            return false;
        }
    }
}
