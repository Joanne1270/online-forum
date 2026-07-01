package com.forum.user.controller;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.BanRequest;
import com.forum.common.dto.Result;
import com.forum.user.dto.*;
import com.forum.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/api/auth/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    @PostMapping("/api/auth/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @PostMapping("/api/auth/forgot/send-code")
    public Result<SendCodeResponse> sendResetCode(@Valid @RequestBody SendCodeRequest request) {
        return Result.ok(userService.sendResetCode(request));
    }

    @PostMapping("/api/auth/forgot/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return Result.ok();
    }

    @GetMapping("/api/users/me")
    public Result<UserProfileVO> me(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        return Result.ok(userService.getProfile(userId));
    }

    @PutMapping("/api/users/me")
    public Result<UserProfileVO> updateMe(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                          @RequestBody ProfileUpdateRequest request) {
        return Result.ok(userService.updateProfile(userId, request));
    }

    @GetMapping("/api/users/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @GetMapping("/api/users/{id}/profile")
    public Result<PublicUserProfileVO> publicProfile(@PathVariable Long id,
                                                     @RequestHeader(value = ForumConstants.HEADER_USER_ID, required = false) Long viewerId,
                                                     @RequestHeader(value = ForumConstants.HEADER_USER_ROLE, required = false) String viewerRole) {
        return Result.ok(userService.getPublicProfile(id, viewerId, viewerRole));
    }

    @GetMapping("/api/users/me/following")
    public Result<List<FollowUserVO>> following(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                                @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        return Result.ok(userService.listFollowing(userId, role));
    }

    @GetMapping("/api/users/me/following-ids")
    public Result<List<Long>> followingIds(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                           @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        return Result.ok(userService.listFollowingIds(userId, role));
    }

    @GetMapping("/api/users/me/followers")
    public Result<List<FollowUserVO>> followers(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                                @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        return Result.ok(userService.listFollowers(userId, role));
    }

    @PostMapping("/api/users/{id}/follow")
    public Result<Void> follow(@PathVariable Long id,
                               @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                               @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        userService.followUser(userId, role, id);
        return Result.ok();
    }

    @DeleteMapping("/api/users/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id,
                                 @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                 @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        userService.unfollowUser(userId, role, id);
        return Result.ok();
    }

    @PutMapping("/api/users/me/privacy")
    public Result<UserProfileVO> updatePrivacy(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                               @RequestBody PrivacyUpdateRequest request) {
        return Result.ok(userService.updatePrivacy(userId, request));
    }

    @GetMapping("/api/users/search")
    public Result<List<UserVO>> search(@RequestParam String keyword,
                                         @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(userService.searchUsers(keyword, limit));
    }

    @PostMapping("/api/users/batch")
    public Result<List<UserVO>> batch(@RequestBody List<String> nicknames) {
        return Result.ok(userService.getByNicknames(nicknames));
    }

    @PutMapping("/api/users/{id}/ban")
    public Result<Void> ban(@PathVariable Long id, @RequestBody(required = false) BanRequest request) {
        userService.banUser(id, request);
        return Result.ok();
    }

    @PutMapping("/api/users/{id}/unban")
    public Result<Void> unban(@PathVariable Long id) {
        userService.unbanUser(id);
        return Result.ok();
    }

    @PutMapping("/api/users/{id}/role")
    public Result<Void> setRole(@PathVariable Long id, @RequestParam String role) {
        userService.setUserRole(id, role);
        return Result.ok();
    }

    @DeleteMapping("/api/users/{id}")
    public Result<Void> deactivate(@PathVariable Long id) {
        userService.deactivateUser(id);
        return Result.ok();
    }

    @GetMapping("/api/users")
    public Result<List<UserVO>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return Result.ok(userService.listUsers(page, size));
    }

    @GetMapping("/api/users/count")
    public Result<Long> count() {
        return Result.ok(userService.countUsers());
    }
}
