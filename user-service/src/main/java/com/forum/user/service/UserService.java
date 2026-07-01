package com.forum.user.service;

import com.forum.common.constant.ForumConstants;
import com.forum.common.exception.BusinessException;
import com.forum.common.ratelimit.RateLimitChecker;
import com.forum.common.ratelimit.RateLimitKeys;
import com.forum.common.util.JwtUtil;
import com.forum.common.util.PhoneUtil;
import com.forum.common.dto.BanRequest;
import com.forum.user.client.PostClient;
import com.forum.user.util.ProfileDisplayUtil;
import com.forum.user.dto.*;
import com.forum.user.entity.User;
import com.forum.user.mapper.UserFollowMapper;
import com.forum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final ProfileChangeService profileChangeService;
    private final UserCacheService userCacheService;
    private final PostClient postClient;
    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthResponse register(RegisterRequest request) {
        String phone = PhoneUtil.normalize(request.getPhone());
        if (!PhoneUtil.isValid(phone)) {
            throw new BusinessException("请输入11位有效手机号");
        }
        String nickname = request.getNickname().trim();
        if (nickname.isEmpty()) {
            throw new BusinessException("昵称不能为空");
        }
        if (userMapper.findByPhone(phone) != null) {
            throw new BusinessException("该手机号已注册");
        }
        if (userMapper.findByNickname(nickname) != null) {
            throw new BusinessException("昵称已被使用");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException("邮箱已注册");
        }
        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(nickname);
        user.setRole(ForumConstants.ROLE_USER);
        user.setStatus(ForumConstants.USER_STATUS_NORMAL);
        user.setAvatar("");
        user.setEmail(email);
        user.setBio("");
        user.setGender(ForumConstants.GENDER_SECRET);
        user.setBirthMonth(null);
        userMapper.insert(user);
        User saved = userMapper.findById(user.getId());
        String token = JwtUtil.generateToken(saved.getId(), saved.getNickname(), saved.getRole());
        return new AuthResponse(token, UserProfileVO.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        String phone = PhoneUtil.normalize(request.getPhone());
        if (!PhoneUtil.isValid(phone)) {
            throw new BusinessException("请输入11位有效手机号");
        }
        assertLoginRateLimit(phone);
        User user = userMapper.findByPhone(phone);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("手机号或密码错误");
        }
        if (user.getStatus() == ForumConstants.USER_STATUS_DELETED) {
            throw new BusinessException("该账号已注销");
        }
        if (user.getStatus() == ForumConstants.USER_STATUS_BANNED) {
            if (isBanExpired(user)) {
                unbanUser(user.getId());
                user = userCacheService.getById(user.getId());
            } else {
                throw new BusinessException(403, buildBanMessage(user));
            }
        }
        String token = JwtUtil.generateToken(user.getId(), user.getNickname(), user.getRole());
        return new AuthResponse(token, UserProfileVO.from(user));
    }

    private void assertLoginRateLimit(String phone) {
        String key = RateLimitKeys.loginWindowKey(phone, 60);
        RateLimitChecker.Counter counter = new RateLimitChecker.Counter() {
            @Override
            public Long increment(String rateKey) {
                return redisTemplate.opsForValue().increment(rateKey);
            }

            @Override
            public void expire(String rateKey, Duration ttl) {
                redisTemplate.expire(rateKey, ttl);
            }
        };
        if (!RateLimitChecker.allow(counter, key, 10, Duration.ofMinutes(1))) {
            throw new BusinessException("登录尝试过于频繁，请稍后再试");
        }
    }

    public SendCodeResponse sendResetCode(SendCodeRequest request) {
        String phone = PhoneUtil.normalize(request.getPhone());
        if (!PhoneUtil.isValid(phone)) {
            throw new BusinessException("请输入11位有效手机号");
        }
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            throw new BusinessException("该手机号尚未注册");
        }
        if (user.getStatus() == ForumConstants.USER_STATUS_DELETED) {
            throw new BusinessException("该账号已注销");
        }
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        if (email.isEmpty()) {
            throw new BusinessException("该账号未绑定邮箱，请先在个人中心绑定邮箱");
        }
        String code = verificationCodeService.sendCode(phone);
        emailService.sendResetCode(email, code);
        return new SendCodeResponse(EmailService.maskEmail(email));
    }

    public void resetPassword(ResetPasswordRequest request) {
        String phone = PhoneUtil.normalize(request.getPhone());
        if (!PhoneUtil.isValid(phone)) {
            throw new BusinessException("请输入11位有效手机号");
        }
        verificationCodeService.verify(phone, request.getCode());
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            throw new BusinessException("该手机号尚未注册");
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.getNewPassword()));
    }

    public UserProfileVO getProfile(Long userId) {
        User user = userCacheService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserProfileVO profile = UserProfileVO.from(user);
        profileChangeService.enrichProfileMeta(userId, profile);
        return profile;
    }

    public UserProfileVO updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userCacheService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            profileChangeService.submitChange(userId, ForumConstants.PROFILE_FIELD_NICKNAME, request.getNickname().trim());
        }
        if (request.getAvatar() != null && !request.getAvatar().equals(user.getAvatar())) {
            profileChangeService.submitChange(userId, ForumConstants.PROFILE_FIELD_AVATAR, request.getAvatar().trim());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getGender() != null) {
            user.setGender(normalizeGender(request.getGender()));
        }
        if (request.getBirthMonth() != null) {
            user.setBirthMonth(parseBirthMonth(request.getBirthMonth()));
        }
        if (request.getEmail() != null || request.getBio() != null || request.getGender() != null
                || request.getBirthMonth() != null) {
            userMapper.updateProfile(user);
            userCacheService.invalidate(userId);
        }
        return getProfile(userId);
    }

    private String normalizeGender(String gender) {
        String value = gender == null ? "" : gender.trim().toUpperCase();
        if (!ForumConstants.GENDER_MALE.equals(value)
                && !ForumConstants.GENDER_FEMALE.equals(value)
                && !ForumConstants.GENDER_SECRET.equals(value)) {
            throw new BusinessException("性别选项无效");
        }
        return value;
    }

    private LocalDate parseBirthMonth(String birthMonth) {
        String text = birthMonth == null ? "" : birthMonth.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() == 7) {
            text = text + "-01";
        }
        try {
            LocalDate date = LocalDate.parse(text);
            if (date.isAfter(LocalDate.now())) {
                throw new BusinessException("出生日期不能晚于当前日期");
            }
            return date;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("出生日期格式不正确");
        }
    }

    public PublicUserProfileVO getPublicProfile(Long targetId, Long viewerId, String viewerRole) {
        User user = userCacheService.getById(targetId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (ForumConstants.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException("无法查看该用户信息");
        }
        if (user.getStatus() != null && user.getStatus() == ForumConstants.USER_STATUS_DELETED) {
            PublicUserProfileVO profile = new PublicUserProfileVO();
            profile.setId(user.getId());
            profile.setNickname("该用户已注销");
            profile.setDeleted(true);
            profile.setPostsVisible(false);
            profile.setFavoritesVisible(false);
            profile.setRepliesVisible(false);
            profile.setFollowingVisible(false);
            profile.setFollowersVisible(false);
            profile.setFollowingCount(0);
            profile.setFollowersCount(0);
            profile.setFollowed(false);
            return profile;
        }
        boolean isSelf = viewerId != null && viewerId.equals(targetId);
        PublicUserProfileVO profile = new PublicUserProfileVO();
        profile.setId(user.getId());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setEmail(user.getEmail());
        profile.setBio(user.getBio());
        profile.setProfileBrief(ProfileDisplayUtil.buildBrief(user.getGender(), user.getBirthMonth()));
        boolean postsVisible = isSelf || isPublicPrivacy(user.getPrivacyPosts());
        boolean favoritesVisible = isSelf || isPublicPrivacy(user.getPrivacyFavorites());
        boolean repliesVisible = isSelf || isPublicPrivacy(user.getPrivacyReplies());
        boolean followingVisible = isSelf || isPublicPrivacy(user.getPrivacyFollowing());
        boolean followersVisible = isSelf || isPublicPrivacy(user.getPrivacyFollowers());
        profile.setPostsVisible(postsVisible);
        profile.setFavoritesVisible(favoritesVisible);
        profile.setRepliesVisible(repliesVisible);
        profile.setFollowingVisible(followingVisible);
        profile.setFollowersVisible(followersVisible);
        profile.setFollowingCount(userFollowMapper.countFollowing(targetId));
        profile.setFollowersCount(userFollowMapper.countFollowers(targetId));
        if (viewerId != null && !isSelf && !ForumConstants.ROLE_ADMIN.equals(viewerRole)) {
            profile.setFollowed(userFollowMapper.exists(viewerId, targetId) > 0);
        }
        if (postsVisible) {
            profile.setPosts(fetchPosts(targetId, viewerId));
        }
        if (favoritesVisible) {
            profile.setFavorites(fetchFavorites(targetId, viewerId));
        }
        if (repliesVisible) {
            profile.setReplies(fetchReplies(targetId, viewerId));
        }
        if (followingVisible) {
            profile.setFollowing(userFollowMapper.listFollowing(targetId));
        }
        if (followersVisible) {
            profile.setFollowers(userFollowMapper.listFollowers(targetId));
        }
        return profile;
    }

    public UserProfileVO updatePrivacy(Long userId, PrivacyUpdateRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        Integer posts = request.getShowPosts() == null ? user.getPrivacyPosts() : toPrivacyValue(request.getShowPosts());
        Integer favorites = request.getShowFavorites() == null ? user.getPrivacyFavorites() : toPrivacyValue(request.getShowFavorites());
        Integer replies = request.getShowReplies() == null ? user.getPrivacyReplies() : toPrivacyValue(request.getShowReplies());
        Integer following = request.getShowFollowing() == null ? user.getPrivacyFollowing() : toPrivacyValue(request.getShowFollowing());
        Integer followers = request.getShowFollowers() == null ? user.getPrivacyFollowers() : toPrivacyValue(request.getShowFollowers());
        userMapper.updatePrivacy(userId, posts, favorites, replies, following, followers);
        userCacheService.invalidate(userId);
        return getProfile(userId);
    }

    private List<java.util.Map<String, Object>> fetchPosts(Long userId, Long viewerId) {
        try {
            var result = postClient.postsByAuthor(userId, 1, 10, viewerId);
            if (result != null && result.getData() != null && result.getData().getList() != null) {
                return result.getData().getList();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private List<java.util.Map<String, Object>> fetchFavorites(Long userId, Long viewerId) {
        try {
            var result = postClient.favoritesByUser(userId, 1, 10, viewerId);
            if (result != null && result.getData() != null && result.getData().getList() != null) {
                return result.getData().getList();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private List<java.util.Map<String, Object>> fetchReplies(Long userId, Long viewerId) {
        try {
            var result = postClient.repliesByAuthor(userId, 1, 10, viewerId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    private boolean isPublicPrivacy(Integer value) {
        return value == null || value == ForumConstants.PRIVACY_PUBLIC;
    }

    private int toPrivacyValue(boolean visible) {
        return visible ? ForumConstants.PRIVACY_PUBLIC : ForumConstants.PRIVACY_HIDDEN;
    }

    public UserVO getById(Long id) {
        User user = userCacheService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserVO.fromPublic(user);
    }

    public List<UserVO> searchUsers(String keyword, int limit) {
        return userMapper.search(keyword, limit).stream().map(UserVO::fromPublic).collect(Collectors.toList());
    }

    public List<UserVO> getByNicknames(List<String> nicknames) {
        if (nicknames == null || nicknames.isEmpty()) {
            return List.of();
        }
        return userMapper.findByNicknames(nicknames).stream().map(UserVO::fromPublic).collect(Collectors.toList());
    }

    public void followUser(Long followerId, String followerRole, Long followeeId) {
        denyAdminFollow(followerRole);
        if (followerId.equals(followeeId)) {
            throw new BusinessException("不能关注自己");
        }
        User followee = userCacheService.getById(followeeId);
        if (followee == null) {
            throw new BusinessException("用户不存在");
        }
        if (ForumConstants.ROLE_ADMIN.equals(followee.getRole())) {
            throw new BusinessException("无法关注管理员");
        }
        if (followee.getStatus() != null && followee.getStatus() == ForumConstants.USER_STATUS_DELETED) {
            throw new BusinessException("该用户已注销");
        }
        if (userFollowMapper.exists(followerId, followeeId) > 0) {
            throw new BusinessException("已经关注过了");
        }
        userFollowMapper.insert(followerId, followeeId);
    }

    public void unfollowUser(Long followerId, String followerRole, Long followeeId) {
        denyAdminFollow(followerRole);
        userFollowMapper.delete(followerId, followeeId);
    }

    public List<FollowUserVO> listFollowing(Long followerId, String followerRole) {
        denyAdminFollow(followerRole);
        return userFollowMapper.listFollowing(followerId);
    }

    public List<Long> listFollowingIds(Long followerId, String followerRole) {
        denyAdminFollow(followerRole);
        return userFollowMapper.listFollowingIds(followerId);
    }

    public List<FollowUserVO> listFollowers(Long followeeId, String followerRole) {
        denyAdminFollow(followerRole);
        return userFollowMapper.listFollowers(followeeId);
    }

    private void denyAdminFollow(String role) {
        if (ForumConstants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException("管理员无法使用关注功能");
        }
    }

    public void banUser(Long id, BanRequest request) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == ForumConstants.USER_STATUS_DELETED) {
            throw new BusinessException("该账号已注销");
        }
        if (ForumConstants.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException("不能封禁管理员账号");
        }
        LocalDateTime bannedUntil = resolveBannedUntil(request);
        userMapper.updateBanStatus(id, ForumConstants.USER_STATUS_BANNED, bannedUntil);
        userCacheService.invalidate(id);
    }

    public void unbanUser(Long id) {
        userMapper.updateBanStatus(id, ForumConstants.USER_STATUS_NORMAL, null);
        userCacheService.invalidate(id);
    }

    public void setUserRole(Long id, String role) {
        if (!ForumConstants.ROLE_ADMIN.equals(role) && !ForumConstants.ROLE_USER.equals(role)) {
            throw new BusinessException("无效的角色");
        }
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == ForumConstants.USER_STATUS_DELETED) {
            throw new BusinessException("该账号已注销");
        }
        userMapper.updateRole(id, role);
        userCacheService.invalidate(id);
    }

    public void deactivateUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == ForumConstants.USER_STATUS_DELETED) {
            throw new BusinessException("该账号已注销");
        }
        if (ForumConstants.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException("不能注销管理员账号");
        }
        userFollowMapper.deleteByUser(id);
        String placeholderPhone = String.format("9%010d", id);
        String placeholderNickname = "已注销用户" + id;
        String invalidPassword = passwordEncoder.encode("deleted_" + id + "_" + System.currentTimeMillis());
        userMapper.deactivateAccount(
                id,
                ForumConstants.USER_STATUS_DELETED,
                placeholderPhone,
                placeholderNickname,
                invalidPassword
        );
        userCacheService.invalidate(id);
    }

    public List<UserVO> listUsers(int page, int size) {
        int offset = (page - 1) * size;
        return userMapper.listAll(offset, size).stream().map(UserVO::from).collect(Collectors.toList());
    }

    public long countUsers() {
        return userMapper.countAll();
    }

    private LocalDateTime resolveBannedUntil(BanRequest request) {
        if (request == null || request.getBanDays() == null || request.getBanDays() < 0) {
            return null;
        }
        if (request.getBanDays() == 0) {
            return null;
        }
        return LocalDateTime.now().plusDays(request.getBanDays());
    }

    private boolean isBanExpired(User user) {
        return user.getBannedUntil() != null && user.getBannedUntil().isBefore(LocalDateTime.now());
    }

    private String buildBanMessage(User user) {
        String remaining = formatRemaining(user.getBannedUntil());
        return "您的账号目前已被封禁，解封剩余时间：" + remaining
                + "\n请遵守社区规则，感谢您对论坛的贡献！";
    }

    private String formatRemaining(LocalDateTime bannedUntil) {
        if (bannedUntil == null) {
            return "永久";
        }
        Duration duration = Duration.between(LocalDateTime.now(), bannedUntil);
        if (duration.isNegative() || duration.isZero()) {
            return "即将解封";
        }
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        if (days > 0) {
            return days + "天" + hours + "小时" + minutes + "分钟";
        }
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        }
        return minutes + "分钟";
    }
}
