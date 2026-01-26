package com.xinyu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResult {
    private boolean success;
    private String errorCode; // "OK", "TOO_MANY_REQUESTS", "TOO_MANY_ATTEMPTS", etc.
    private String message;
    private String verificationId;
    private int ttlSeconds;
    private int cooldownSeconds;

    public static SendResult ok(String vid, int ttl, int cd) {
        return SendResult.builder()
                .success(true)
                .errorCode("OK")
                .message("成功发送")
                .verificationId(vid)
                .ttlSeconds(ttl)
                .cooldownSeconds(cd)
                .build();
    }

    public static SendResult tooMany(int cooldownLeft) {
        return SendResult.builder()
                .success(false)
                .errorCode("TOO_MANY_REQUESTS")
                .message("请求过于频繁，请稍候")
                .cooldownSeconds(cooldownLeft)
                .build();
    }

    public static SendResult blocked(int lockLeft) {
        return SendResult.builder()
                .success(false)
                .errorCode("TOO_MANY_ATTEMPTS")
                .message("接收器暂时被阻止")
                .cooldownSeconds(lockLeft)
                .build();
    }
}
