package com.xinyu.util;

import com.xinyu.config.VerifyCodeProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class VerifyCodeMailSender {
    private final JavaMailSender mailSender;
    private final VerifyCodeProperties props;

    public VerifyCodeMailSender(JavaMailSender mailSender, VerifyCodeProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    public void sendVerifyCodeMail(String to, String scene, String code, int ttlSeconds) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.getMailFrom());
        msg.setTo(to);
        
        String subject = getSubjectByScene(scene);
        msg.setSubject(subject);
        
        String text = String.format("您的验证码是：%s，有效期为 %d 分钟。该验证码用于 %s 场景，如非本人操作请忽略。", 
                code, ttlSeconds / 60, getSceneName(scene));
        msg.setText(text);
        
        mailSender.send(msg);
    }

    private String getSubjectByScene(String scene) {
        return switch (scene) {
            case "REGISTER" -> "【Xinyu Blog】注册验证码";
            case "LOGIN" -> "【Xinyu Blog】登录验证码";
            case "RESET_PASSWORD" -> "【Xinyu Blog】找回密码验证码";
            case "BIND" -> "【Xinyu Blog】绑定验证码";
            case "CHANGE" -> "【Xinyu Blog】变更验证码";
            default -> "【Xinyu Blog】验证码";
        };
    }

    private String getSceneName(String scene) {
        return switch (scene) {
            case "REGISTER" -> "注册账号";
            case "LOGIN" -> "登录";
            case "RESET_PASSWORD" -> "重置密码";
            case "BIND" -> "绑定操作";
            case "CHANGE" -> "修改信息";
            default -> "安全验证";
        };
    }
}
