package com.forum.admin.client;

import com.forum.admin.dto.AdminBoardVO;
import com.forum.admin.dto.AdminBannedWordVO;
import com.forum.admin.dto.AdminPostVO;
import com.forum.admin.dto.AdminReportVO;
import com.forum.common.dto.PageResult;
import com.forum.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "post-service")
public interface PostAdminClient {

    @GetMapping("/api/boards")
    Result<List<AdminBoardVO>> boards();

    @PostMapping("/api/boards")
    Result<AdminBoardVO> createBoard(@RequestBody AdminBoardVO board);

    @PutMapping("/api/boards/{id}")
    Result<AdminBoardVO> updateBoard(@PathVariable("id") Long id, @RequestBody AdminBoardVO board);

    @DeleteMapping("/api/boards/{id}")
    Result<Void> deleteBoard(@PathVariable("id") Long id);

    @GetMapping("/api/posts")
    Result<PageResult<AdminPostVO>> listPosts(@RequestParam(value = "boardId", required = false) Long boardId,
                                              @RequestParam("page") int page,
                                              @RequestParam("size") int size);

    @DeleteMapping("/api/posts/{id}")
    Result<Void> deletePost(@PathVariable("id") Long id,
                            @RequestHeader("X-User-Id") Long userId,
                            @RequestHeader("X-User-Role") String role);

    @GetMapping("/api/posts/count")
    Result<Long> countPosts();

    @GetMapping("/api/reports")
    Result<PageResult<AdminReportVO>> listReports(@RequestParam("page") int page,
                                                  @RequestParam("size") int size);

    @PutMapping("/api/reports/{id}/handle")
    Result<Void> handleReport(@PathVariable("id") Long id, @RequestParam("action") String action);

    @GetMapping("/api/banned-words")
    Result<List<AdminBannedWordVO>> listBannedWords();

    @PostMapping("/api/banned-words")
    Result<AdminBannedWordVO> addBannedWord(@RequestBody AdminBannedWordVO request);

    @DeleteMapping("/api/banned-words/{id}")
    Result<Void> deleteBannedWord(@PathVariable("id") Long id);
}
