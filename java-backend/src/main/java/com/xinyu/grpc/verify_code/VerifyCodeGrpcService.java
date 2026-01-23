package com.xinyu.grpc.verify_code;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class VerifyCodeGrpcService
        extends VerifyCodeServiceGrpc.VerifyCodeServiceImplBase {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] DIGITS = "0123456789".toCharArray();

    /** 验证码有效期：60 秒 */
    private static final int VERIFY_CODE_LIFE = 60;

    @Override
    public void getVerifyCode(GetVerifyCodeRequest request,
                              StreamObserver<GetVerifyCodeReply> responseObserver) {

        // ✅ 六位纯数字验证码
        String verifyCode = generateSixDigitCode();

        // ✅ 生成时间（秒级时间戳）
        int verifyCodeTime = (int) Instant.now().getEpochSecond();

        GetVerifyCodeReply reply = GetVerifyCodeReply.newBuilder()
                .setCode(0L)                            // 状态码：成功
                .setVerifyCode(verifyCode)              // 验证码
                .setVerifyCodeTime(verifyCodeTime)      // 生成时间
                .setVerifyCodeLife(VERIFY_CODE_LIFE)    // 生命周期 60s
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    /**
     * 生成 6 位纯数字验证码
     */
    private String generateSixDigitCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(DIGITS[RANDOM.nextInt(DIGITS.length)]);
        }
        return sb.toString();
    }
}
