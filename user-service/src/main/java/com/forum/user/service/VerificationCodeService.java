package com.forum.user.service;

import com.forum.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {

    private static final long EXPIRE_SECONDS = 300;
    private static final long RESEND_SECONDS = 60;

    private final Map<String, CodeEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String sendCode(String phone) {
        CodeEntry existing = store.get(phone);
        long now = Instant.now().getEpochSecond();
        if (existing != null && now - existing.sentAt < RESEND_SECONDS) {
            throw new BusinessException("发送过于频繁，请稍后再试");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(phone, new CodeEntry(code, now));
        return code;
    }

    public void verify(String phone, String code) {
        CodeEntry entry = store.get(phone);
        long now = Instant.now().getEpochSecond();
        if (entry == null || now - entry.sentAt > EXPIRE_SECONDS) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!entry.code.equals(code == null ? "" : code.trim())) {
            throw new BusinessException("验证码错误");
        }
        store.remove(phone);
    }

    private record CodeEntry(String code, long sentAt) {
    }
}
