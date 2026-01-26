package com.xinyu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResult {
    private boolean verified;
    private String errorCode;
    private String message;
    private int remainingAttempts;
    private int lockSeconds;

    public static VerifyResult verified() {
        return VerifyResult.builder()
                .verified(true)
                .errorCode("OK")
                .message("校验成功")
                .build();
    }

    public static VerifyResult mismatch(int remaining) {
        return VerifyResult.builder()
                .verified(false)
                .errorCode("CODE_MISMATCH")
                .message("验证码错误")
                .remainingAttempts(remaining)
                .build();
    }

    public static VerifyResult tooManyAttempts(int lockSeconds) {
        return VerifyResult.builder()
                .verified(false)
                .errorCode("TOO_MANY_ATTEMPTS")
                .message("尝试次数过多，已锁定")
                .remainingAttempts(0)
                .lockSeconds(lockSeconds)
                .build();
    }

    public static VerifyResult locked(int lockLeft) {
        return VerifyResult.builder()
                .verified(false)
                .errorCode("TOO_MANY_ATTEMPTS")
                .message("账户目前已被锁定")
                .remainingAttempts(0)
                .lockSeconds(lockLeft)
                .build();
    }

    public static VerifyResult notFoundOrExpired() {
        return VerifyResult.builder()
                .verified(false)
                .errorCode("EXPIRED")
                .message("未找到验证 ID 或验证 ID 已过期")
                .build();
    }
}
