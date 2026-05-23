package com.ccc.zerocodegenerateproject.common.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SHA256 {
    /**
     * 生成随机盐值
     */
    public static String getSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * SHA-256 加盐加密
     * @param password 明文密码
     * @param salt 盐值
     */
    public static String encode(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 将盐值和密码拼接后计算哈希
            String saltedPassword = salt + password;
            byte[] hash = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));

            // 转为 Base64 字符串存储
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不存在", e);
        }
    }
}
