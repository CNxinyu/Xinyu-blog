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
                .message("Verified successfully")
                .build();
    }

    public static VerifyResult mismatch(int remaining) {
        return VerifyResult.builder()
                .verified(false)
                .errorCode("CODE_MISMATCH")
                .message("Code mismatch")
                .remainingAttempts(remaining)
                .build();
    }

    public static VerifyResult tooManyAttempts(int lockSeconds) {
        return VerifyResult.builder()
                .verified(false)
                .errorCode("TOO_MANY_ATTEMPTS")
                .message("Too many failed attempts, locked")
                .remainingAttempts(0)
                .lockSeconds(lockSeconds)
                .build();
    }

    public static VerifyResult locked(int lockLeft) {
        return VerifyResult.builder()
                .verified(false)
                .errorCode("TOO_MANY_ATTEMPTS")
                .message("Account is currently locked")
                .remainingAttempts(0)
                .lockSeconds(lockLeft)
                .build();
    }

    public static VerifyResult notFoundOrExpired() {
        return VerifyResult.builder()
                .verified(false)
                .errorCode("EXPIRED")
                .message("Verification ID not found or expired")
                .build();
    }
}
