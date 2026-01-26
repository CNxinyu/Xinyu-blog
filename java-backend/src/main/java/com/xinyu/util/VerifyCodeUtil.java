package com.xinyu.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 验证码生成 + token + HMAC hash
 */

public class VerifyCodeUtil {
    private static final SecureRandom RND = new SecureRandom();

    public static String gen6Digit() {
        int n = RND.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    public static String genVerificationId() {
        byte[] b = new byte[24]; // 192-bit
        RND.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String hmacSha256(String secret, String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 常量时间比较，避免时序攻击（基础防护）
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        int la = a.length(), lb = b.length();
        int r = la ^ lb;
        for (int i = 0; i < Math.min(la, lb); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
