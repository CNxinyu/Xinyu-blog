package com.xinyu.service;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import com.xinyu.config.VerifyCodeProperties;
import com.xinyu.model.SendResult;
import com.xinyu.model.TokenPayload;
import com.xinyu.model.VerifyResult;
import com.xinyu.repo.VerifyCodeRedisRepo;
import com.xinyu.util.VerifyCodeMailSender;
import com.xinyu.util.VerifyCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VerifyCodeServiceLogic {

    private final VerifyCodeProperties props;
    private final VerifyCodeRedisRepo repo;
    private final VerifyCodeMailSender mailSender;

    private static final Pattern  EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public VerifyCodeServiceLogic(VerifyCodeProperties props, VerifyCodeRedisRepo repo, VerifyCodeMailSender mailSender) {
        this.props = props;
        this.repo = repo;
        this.mailSender = mailSender;
    }

    public SendResult sendEmailCode(String receiverEmail, String scene) {
        // 0) 格式校验
        if (receiverEmail == null || !EMAIL_PATTERN.matcher(receiverEmail).matches()) {
            return SendResult.builder()
                    .success(false)
                    .errorCode("INVALID_ARGUMENT")
                    .message("邮箱格式不合法")
                    .build();
        }

        // 1) lock 检查
        if (repo.exists(repo.lockKey(receiverEmail))) {
            long left = repo.ttlSeconds(repo.lockKey(receiverEmail)).orElse(0L);
            return SendResult.blocked((int) left);
        }

        // 2) cooldown 检查
        if (repo.exists(repo.cdKey(receiverEmail))) {
            long left = repo.ttlSeconds(repo.cdKey(receiverEmail)).orElse((long) props.getCooldownSeconds());
            return SendResult.tooMany((int) left);
        }

        // 3) 生成 code + verificationId
        String code = VerifyCodeUtil.gen6Digit();
        String vid = VerifyCodeUtil.genVerificationId();

        // 4) 存 token（hash后）
        TokenPayload payload = TokenPayload.builder()
                .receiver(receiverEmail)
                .scene(scene)
                .sentAtEpochSec(Instant.now().getEpochSecond())
                .codeHash(VerifyCodeUtil.hmacSha256(props.getHmacSecret(), vid + ":" + code))
                .verified(false)
                .build();

        repo.set(repo.tokenKey(vid), repo.toJson(payload), Duration.ofSeconds(props.getTtlSeconds()));
        repo.set(repo.cdKey(receiverEmail), "1", Duration.ofSeconds(props.getCooldownSeconds()));

        // 5) 发邮件
        try {
            mailSender.sendVerifyCodeMail(receiverEmail, scene, code, props.getTtlSeconds());
            log.info("发送验证码到 {}: 场景={}, vid={}", receiverEmail, scene, vid);
        } catch (Exception e) {
            log.error("发送邮件失败: {}", receiverEmail, e);
            repo.del(repo.tokenKey(vid));
            repo.del(repo.cdKey(receiverEmail));
            throw e;
        }

        return SendResult.ok(vid, props.getTtlSeconds(), props.getCooldownSeconds());
    }

    public VerifyResult verify(String verificationId, String code) {
        // 1) token 必须存在
        var tokenOpt = repo.get(repo.tokenKey(verificationId));
        if (tokenOpt.isEmpty()) {
            return VerifyResult.notFoundOrExpired();
        }

        TokenPayload payload = repo.fromJson(tokenOpt.get(), TokenPayload.class);

        // 2) receiver lock 检查
        if (repo.exists(repo.lockKey(payload.getReceiver()))) {
            long left = repo.ttlSeconds(repo.lockKey(payload.getReceiver())).orElse(0L);
            return VerifyResult.locked((int) left);
        }

        // 3) 比对 hash
        String expected = payload.getCodeHash();
        String actual = VerifyCodeUtil.hmacSha256(props.getHmacSecret(), verificationId + ":" + code);

        boolean ok = VerifyCodeUtil.constantTimeEquals(expected, actual);
        if (ok) {
            // 校验成功：标记为 verified，更新 Redis
            payload.setVerified(true);
            repo.set(repo.tokenKey(verificationId), repo.toJson(payload), Duration.ofSeconds(props.getTtlSeconds()));
            // 清理尝试次数
            repo.del(repo.tryKey(verificationId));
            return VerifyResult.verified();
        }

        // 4) 失败计数 + 锁定
        long attempts = repo.incr(repo.tryKey(verificationId), Duration.ofSeconds(props.getTtlSeconds()));
        int remaining = Math.max(props.getMaxAttempts() - (int) attempts, 0);

        if (attempts >= props.getMaxAttempts()) {
            repo.set(repo.lockKey(payload.getReceiver()), "1", Duration.ofSeconds(props.getLockSeconds()));
            return VerifyResult.tooManyAttempts(props.getLockSeconds());
        }

        return VerifyResult.mismatch(remaining);
    }

    public boolean consumeCode(String verificationId) {
        var tokenOpt = repo.get(repo.tokenKey(verificationId));
        if (tokenOpt.isEmpty()) {
            return false;
        }

        TokenPayload payload = repo.fromJson(tokenOpt.get(), TokenPayload.class);
        if (payload.isVerified()) {
            // 消费掉：直接删除
            repo.del(repo.tokenKey(verificationId));
            return true;
        }
        return false;
    }
}
