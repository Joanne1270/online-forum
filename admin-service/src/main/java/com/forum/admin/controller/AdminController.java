package com.forum.admin.controller;

import com.forum.admin.client.PostAdminClient;
import com.forum.admin.client.UserAdminClient;
import com.forum.admin.dto.AdminBoardVO;
import com.forum.admin.dto.AdminBannedWordVO;
import com.forum.admin.dto.AdminPostVO;
import com.forum.admin.dto.AdminProfileRequestVO;
import com.forum.admin.dto.AdminReportVO;
import com.forum.admin.dto.AdminUserVO;
import com.forum.admin.dto.OnlineStatsVO;
import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.BanRequest;
import com.forum.common.dto.PageResult;
import com.forum.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final UserAdminClient userAdminClient;
    private final PostAdminClient postAdminClient;

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("userCount", userAdminClient.countUsers().getData());
        stats.put("postCount", postAdminClient.countPosts().getData());
        return Result.ok(stats);
    }

    @GetMapping("/online-stats")
    public Result<OnlineStatsVO> onlineStats(@RequestParam(defaultValue = "day") String range) {
        return userAdminClient.onlineStats(range);
    }

    @GetMapping("/users")
    public Result<List<AdminUserVO>> users(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return userAdminClient.listUsers(page, size);
    }

    @PutMapping("/users/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id, @RequestBody(required = false) BanRequest request) {
        return userAdminClient.ban(id, request);
    }

    @PutMapping("/users/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        return userAdminClient.unban(id);
    }

    @PutMapping("/users/{id}/role")
    public Result<Void> setUserRole(@PathVariable Long id, @RequestParam String role) {
        return userAdminClient.setRole(id, role);
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deactivateUser(@PathVariable Long id) {
        return userAdminClient.deactivate(id);
    }

    @GetMapping("/boards")
    public Result<List<AdminBoardVO>> boards() {
        return postAdminClient.boards();
    }

    @PostMapping("/boards")
    public Result<AdminBoardVO> createBoard(@RequestBody AdminBoardVO board) {
        return postAdminClient.createBoard(board);
    }

    @PutMapping("/boards/{id}")
    public Result<AdminBoardVO> updateBoard(@PathVariable Long id, @RequestBody AdminBoardVO board) {
        return postAdminClient.updateBoard(id, board);
    }

    @DeleteMapping("/boards/{id}")
    public Result<Void> deleteBoard(@PathVariable Long id) {
        return postAdminClient.deleteBoard(id);
    }

    @GetMapping("/posts")
    public Result<PageResult<AdminPostVO>> posts(@RequestParam(required = false) Long boardId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return postAdminClient.listPosts(boardId, page, size);
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id,
                                   @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                   @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        return postAdminClient.deletePost(id, userId, role);
    }

    @GetMapping("/reports")
    public Result<PageResult<AdminReportVO>> reports(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return postAdminClient.listReports(page, size);
    }

    @PutMapping("/reports/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id, @RequestParam String action) {
        return postAdminClient.handleReport(id, action);
    }

    @GetMapping("/profile-requests")
    public Result<PageResult<AdminProfileRequestVO>> profileRequests(@RequestParam(defaultValue = "1") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return userAdminClient.listProfileRequests(page, size);
    }

    @PutMapping("/profile-requests/{id}/approve")
    public Result<Void> approveProfileRequest(@PathVariable Long id) {
        return userAdminClient.approveProfileRequest(id);
    }

    @PutMapping("/profile-requests/{id}/reject")
    public Result<Void> rejectProfileRequest(@PathVariable Long id) {
        return userAdminClient.rejectProfileRequest(id);
    }

    @GetMapping("/banned-words")
    public Result<List<AdminBannedWordVO>> bannedWords() {
        return postAdminClient.listBannedWords();
    }

    @PostMapping("/banned-words")
    public Result<AdminBannedWordVO> addBannedWord(@RequestBody AdminBannedWordVO request) {
        return postAdminClient.addBannedWord(request);
    }

    @DeleteMapping("/banned-words/{id}")
    public Result<Void> deleteBannedWord(@PathVariable Long id) {
        return postAdminClient.deleteBannedWord(id);
    }
}
