package com.forum.user.service;

import com.forum.common.exception.BusinessException;
import com.forum.user.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public void sendResetCode(String email, String code) {
        if (mailPassword == null || mailPassword.isBlank()) {
            log.info("[forum-dev] reset code for {}: {} (邮箱未配置授权码，仅打印日志)", email, code);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.getFrom(), mailProperties.getFromName());
            helper.setTo(email);
            helper.setSubject("密码重置验证码");
            helper.setText(buildBody(code), true);
            mailSender.send(message);
            log.info("Reset code email sent to {}", maskEmail(email));
        } catch (Exception ex) {
            log.error("Failed to send reset code email to {}", maskEmail(email), ex);
            throw new BusinessException("验证码邮件发送失败，请稍后重试");
        }
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + parts[1];
        }
        return local.substring(0, 2) + "***@" + parts[1];
    }

    private String buildBody(String code) {
        return "<p>您正在重置在线社区论坛账号密码，验证码为：</p>"
                + "<p style=\"font-size:24px;font-weight:bold;letter-spacing:4px;\">" + code + "</p>"
                + "<p>验证码 5 分钟内有效，请勿泄露给他人。</p>";
    }
}
