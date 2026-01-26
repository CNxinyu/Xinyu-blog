package com.xinyu.grpc;

import com.xinyu.config.VerifyCodeProperties;
import com.xinyu.service.VerifyCodeServiceLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;

@Slf4j
@Service
public class VerifyCodeGrpcImpl extends VerifyCodeServiceGrpc.VerifyCodeServiceImplBase {

    private final VerifyCodeServiceLogic logic;
    private final VerifyCodeProperties props;

    public VerifyCodeGrpcImpl(VerifyCodeServiceLogic logic, VerifyCodeProperties props) {
        this.logic = logic;
        this.props = props;
    }

    @Override
    public void sendVerifyCode(SendVerifyCodeRequest request,
                               io.grpc.stub.StreamObserver<SendVerifyCodeReply> responseObserver) {

        log.info("已收到 SendVerifyCode 请求 for: receiver={}, channel={}", request.getReceiver(), request.getChannel());

        // 仅做邮件通道（基础版）
        if (request.getChannel() != Channel.EMAIL) {
            responseObserver.onNext(SendVerifyCodeReply.newBuilder()
                    .setCode(ErrorCode.INVALID_ARGUMENT)
                    .setMessage("目前只开放邮箱验证")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        var r = logic.sendEmailCode(request.getReceiver(), request.getScene().name());

        SendVerifyCodeReply.Builder b = SendVerifyCodeReply.newBuilder();
        if (r.isSuccess()) {
            b.setCode(ErrorCode.OK)
                    .setMessage("OK")
                    .setVerificationId(r.getVerificationId())
                    .setSentAt(tsNow())
                    .setTtl(Duration.newBuilder().setSeconds(r.getTtlSeconds()).build())
                    .setCooldown(Duration.newBuilder().setSeconds(r.getCooldownSeconds()).build())
                    .setRiskAction(RiskAction.ALLOW);
        } else {
            ErrorCode code = switch (r.getErrorCode()) {
                case "TOO_MANY_REQUESTS" -> ErrorCode.TOO_MANY_REQUESTS;
                case "TOO_MANY_ATTEMPTS" -> ErrorCode.TOO_MANY_ATTEMPTS;
                case "INVALID_ARGUMENT" -> ErrorCode.INVALID_ARGUMENT;
                default -> ErrorCode.INTERNAL_ERROR;
            };
            b.setCode(code)
                    .setMessage(r.getMessage())
                    .setCooldown(Duration.newBuilder().setSeconds(r.getCooldownSeconds()).build())
                    .setRiskAction(code == ErrorCode.TOO_MANY_ATTEMPTS ? RiskAction.BLOCK : RiskAction.ALLOW);
        }

        responseObserver.onNext(b.build());
        responseObserver.onCompleted();
    }

    @Override
    public void verifyCode(VerifyCodeRequest request,
                           io.grpc.stub.StreamObserver<VerifyCodeReply> responseObserver) {

        log.info("已收到 SendVerifyCode 请求 vid: {}", request.getVerificationId());

        var r = logic.verify(request.getVerificationId(), request.getCode());

        VerifyCodeReply.Builder b = VerifyCodeReply.newBuilder();
        if (r.isVerified()) {
            b.setCode(ErrorCode.OK)
                    .setMessage("OK")
                    .setVerified(true)
                    .setVerifiedAt(tsNow())
                    .setRiskAction(RiskAction.ALLOW);
        } else {
            ErrorCode code = switch (r.getErrorCode()) {
                case "CODE_MISMATCH" -> ErrorCode.CODE_MISMATCH;
                case "TOO_MANY_ATTEMPTS" -> ErrorCode.TOO_MANY_ATTEMPTS;
                case "EXPIRED" -> ErrorCode.EXPIRED;
                default -> ErrorCode.INTERNAL_ERROR;
            };
            b.setCode(code)
                    .setMessage(r.getMessage())
                    .setVerified(false)
                    .setRemainingAttempts(r.getRemainingAttempts())
                    .setLockDuration(Duration.newBuilder().setSeconds(r.getLockSeconds()).build())
                    .setRiskAction(code == ErrorCode.TOO_MANY_ATTEMPTS ? RiskAction.BLOCK : RiskAction.ALLOW);
        }

        responseObserver.onNext(b.build());
        responseObserver.onCompleted();
    }

    @Override
    public void consumeCode(ConsumeCodeRequest request,
                            io.grpc.stub.StreamObserver<ConsumeCodeReply> responseObserver) {

        log.info("收到 ConsumeCode 请求 vid: {}", request.getVerificationId());

        boolean ok = logic.consumeCode(request.getVerificationId());

        ConsumeCodeReply.Builder b = ConsumeCodeReply.newBuilder();
        if (ok) {
            b.setCode(ErrorCode.OK)
                    .setMessage("OK")
                    .setConsumed(true)
                    .setConsumedAt(tsNow());
        } else {
            b.setCode(ErrorCode.NOT_FOUND)
                    .setMessage("代码未验证或已被使用/过期")
                    .setConsumed(false);
        }

        responseObserver.onNext(b.build());
        responseObserver.onCompleted();
    }

    private static Timestamp tsNow() {
        Instant now = Instant.now();
        return Timestamp.newBuilder().setSeconds(now.getEpochSecond()).setNanos(now.getNano()).build();
    }
}
