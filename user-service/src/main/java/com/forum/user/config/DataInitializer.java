package com.forum.user.config;

import com.forum.common.constant.ForumConstants;
import com.forum.user.mapper.UserMapper;
import com.forum.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        ensureUser("13800000001", "admin123", "管理员", ForumConstants.ROLE_ADMIN);
        ensureUser("13800000002", "demo123", "演示用户", ForumConstants.ROLE_USER);
    }

    private void ensureUser(String phone, String password, String nickname, String role) {
        if (userMapper.findByPhone(phone) != null) {
            return;
        }
        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(ForumConstants.USER_STATUS_NORMAL);
        user.setAvatar("");
        user.setEmail("");
        user.setBio("");
        userMapper.insert(user);
    }
}
