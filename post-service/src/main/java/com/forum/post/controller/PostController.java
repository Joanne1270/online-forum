package com.forum.post.controller;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.PageResult;
import com.forum.common.dto.Result;
import com.forum.post.dto.*;
import com.forum.post.entity.Board;
import com.forum.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/api/boards")
    public Result<List<BoardVO>> boards() {
        return Result.ok(postService.listBoards());
    }

    @PostMapping("/api/boards")
    public Result<Board> createBoard(@RequestBody Board board) {
        return Result.ok(postService.createBoard(board));
    }

    @PutMapping("/api/boards/{id}")
    public Result<Board> updateBoard(@PathVariable("id") Long id, @RequestBody Board board) {
        board.setId(id);
        return Result.ok(postService.updateBoard(board));
    }

    @DeleteMapping("/api/boards/{id}")
    public Result<Void> deleteBoard(@PathVariable("id") Long id) {
        postService.deleteBoard(id);
        return Result.ok();
    }

    @GetMapping("/api/posts")
    public Result<PageResult<PostVO>> listPosts(@RequestParam(required = false) Long boardId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(defaultValue = "time_desc") String sort,
                                                 @RequestParam(required = false) Boolean featuredOnly,
                                                 @RequestHeader(value = ForumConstants.HEADER_USER_ID, required = false) Long userId) {
        return Result.ok(postService.listPosts(boardId, page, size, userId, sort, featuredOnly));
    }

    @GetMapping("/api/posts/search")
    public Result<PageResult<PostVO>> search(@RequestParam String keyword,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestHeader(value = ForumConstants.HEADER_USER_ID, required = false) Long userId) {
        return Result.ok(postService.searchPosts(keyword, page, size, userId));
    }

    @GetMapping("/api/posts/hot")
    public Result<List<PostVO>> hot(@RequestParam(required = false) Long boardId,
                                    @RequestParam(defaultValue = "10") int limit,
                                    @RequestHeader(value = ForumConstants.HEADER_USER_ID, required = false) Long userId) {
        return Result.ok(postService.hotPosts(boardId, limit, userId));
    }

    @GetMapping("/api/posts/favorites")
    public Result<PageResult<PostVO>> favorites(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return Result.ok(postService.listFavorites(userId, page, size));
    }

    @GetMapping("/api/posts/mine")
    public Result<PageResult<PostVO>> myPosts(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.ok(postService.myPosts(userId, page, size));
    }

    @GetMapping("/api/posts/drafts/mine")
    public Result<PageResult<PostVO>> myDrafts(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return Result.ok(postService.listDrafts(userId, page, size));
    }

    @GetMapping("/api/posts/drafts/{id}")
    public Result<PostVO> draftDetail(@PathVariable("id") Long id,
                                      @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        return Result.ok(postService.getDraftDetail(id, userId));
    }

    @PostMapping("/api/posts/drafts")
    public Result<PostVO> createDraft(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                      @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                      @RequestBody PostDraftRequest request) {
        return Result.ok(postService.createDraft(userId, role, request));
    }

    @PutMapping("/api/posts/drafts/{id}")
    public Result<PostVO> updateDraft(@PathVariable("id") Long id,
                                      @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                      @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                      @RequestBody PostDraftRequest request) {
        return Result.ok(postService.updateDraft(id, userId, role, request));
    }

    @PostMapping("/api/posts/drafts/{id}/publish")
    public Result<PostVO> publishDraft(@PathVariable("id") Long id,
                                       @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                       @RequestHeader(ForumConstants.HEADER_NICKNAME) String nickname,
                                       @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                       @RequestBody(required = false) PostDraftRequest request) {
        return Result.ok(postService.publishDraft(id, userId, nickname, role, request));
    }

    @DeleteMapping("/api/posts/drafts/{id}")
    public Result<Void> deleteDraft(@PathVariable("id") Long id,
                                    @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        postService.deleteDraft(id, userId);
        return Result.ok();
    }

    @GetMapping("/api/posts/following-feed")
    public Result<PageResult<PostVO>> followingFeed(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                                   @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.ok(postService.listFollowingFeed(userId, role, page, size));
    }

    @GetMapping("/api/posts/likes/mine")
    public Result<PageResult<PostVO>> myLikes(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.ok(postService.listMyLikes(userId, page, size));
    }

    @GetMapping("/api/reports/mine")
    public Result<PageResult<UserReportVO>> myReports(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return Result.ok(postService.listMyReports(userId, page, size));
    }

    @GetMapping("/api/posts/count")
    public Result<Long> count() {
        return Result.ok(postService.countPosts());
    }

    @GetMapping("/api/posts/{id}")
    public Result<PostVO> detail(@PathVariable("id") Long id,
                                 @RequestHeader(value = ForumConstants.HEADER_USER_ID, required = false) Long userId) {
        return Result.ok(postService.getPostDetail(id, userId));
    }

    @GetMapping("/api/posts/{id}/exists")
    public Result<Boolean> exists(@PathVariable("id") Long id) {
        return Result.ok(postService.postExists(id));
    }

    @PostMapping("/api/posts/check-similar")
    public Result<PostSimilarCheckVO> checkSimilar(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                                   @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                                   @RequestBody PostSimilarCheckRequest request) {
        return Result.ok(postService.checkSimilarPost(userId, role, request));
    }

    @PostMapping("/api/posts")
    public Result<PostVO> create(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                 @RequestHeader(ForumConstants.HEADER_NICKNAME) String nickname,
                                 @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                 @Valid @RequestBody PostCreateRequest request) {
        return Result.ok(postService.createPost(userId, nickname, role, request));
    }

    @PutMapping("/api/posts/{id}")
    public Result<PostVO> update(@PathVariable("id") Long id,
                                   @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                   @RequestBody PostUpdateRequest request) {
        return Result.ok(postService.updatePost(id, userId, request));
    }

    @DeleteMapping("/api/posts/{id}")
    public Result<Void> delete(@PathVariable("id") Long id,
                               @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                               @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.deletePost(id, userId, ForumConstants.ROLE_ADMIN.equals(role));
        return Result.ok();
    }

    @PostMapping("/api/posts/{id}/like")
    public Result<Void> likePost(@PathVariable("id") Long id,
                                 @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                 @RequestHeader(ForumConstants.HEADER_NICKNAME) String nickname,
                                 @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.likePost(id, userId, nickname, role);
        return Result.ok();
    }

    @DeleteMapping("/api/posts/{id}/like")
    public Result<Void> unlikePost(@PathVariable("id") Long id,
                                   @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                   @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.unlikePost(id, userId, role);
        return Result.ok();
    }

    @PostMapping("/api/posts/{id}/favorite")
    public Result<Void> favoritePost(@PathVariable("id") Long id,
                                     @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                     @RequestHeader(ForumConstants.HEADER_NICKNAME) String nickname,
                                     @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.favoritePost(id, userId, nickname, role);
        return Result.ok();
    }

    @DeleteMapping("/api/posts/{id}/favorite")
    public Result<Void> unfavoritePost(@PathVariable("id") Long id,
                                       @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                       @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.unfavoritePost(id, userId, role);
        return Result.ok();
    }

    @PostMapping("/api/posts/{id}/report")
    public Result<Void> reportPost(@PathVariable("id") Long id,
                                   @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                   @Valid @RequestBody ReportCreateRequest request) {
        postService.reportPost(id, userId, request);
        return Result.ok();
    }

    @GetMapping("/api/posts/{postId}/replies")
    public Result<List<ReplyVO>> replies(@PathVariable("postId") Long postId,
                                         @RequestParam(defaultValue = "time") String sort,
                                         @RequestHeader(value = ForumConstants.HEADER_USER_ID, required = false) Long userId) {
        return Result.ok(postService.listReplies(postId, userId, sort));
    }

    @PostMapping("/api/posts/{postId}/replies")
    public Result<ReplyVO> createReply(@PathVariable("postId") Long postId,
                                       @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                       @RequestHeader(ForumConstants.HEADER_NICKNAME) String nickname,
                                       @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                       @Valid @RequestBody ReplyCreateRequest request) {
        return Result.ok(postService.createReply(postId, userId, nickname, role, request));
    }

    @PostMapping("/api/replies/{id}/like")
    public Result<Void> likeReply(@PathVariable("id") Long id,
                                  @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                  @RequestHeader(ForumConstants.HEADER_NICKNAME) String nickname,
                                  @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.likeReply(id, userId, nickname, role);
        return Result.ok();
    }

    @DeleteMapping("/api/replies/{id}/like")
    public Result<Void> unlikeReply(@PathVariable("id") Long id,
                                    @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                    @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.unlikeReply(id, userId, role);
        return Result.ok();
    }

    @PostMapping("/api/replies/{id}/dislike")
    public Result<Void> dislikeReply(@PathVariable("id") Long id,
                                     @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                     @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.dislikeReply(id, userId, role);
        return Result.ok();
    }

    @DeleteMapping("/api/replies/{id}/dislike")
    public Result<Void> undislikeReply(@PathVariable("id") Long id,
                                       @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                       @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.undislikeReply(id, userId, role);
        return Result.ok();
    }

    @GetMapping("/api/replies/mine")
    public Result<List<ReplyVO>> myReplies(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.ok(postService.myReplies(userId, page, size));
    }

    @GetMapping("/api/replies/{id}/post-id")
    public Result<Long> replyPostId(@PathVariable("id") Long id) {
        return Result.ok(postService.getReplyPostId(id));
    }

    @GetMapping("/api/replies/{id}/preview")
    public Result<String> replyPreview(@PathVariable("id") Long id) {
        return Result.ok(postService.getReplyPreview(id));
    }

    @DeleteMapping("/api/replies/{id}")
    public Result<Void> deleteReply(@PathVariable("id") Long id,
                                    @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        postService.deleteReply(id, userId);
        return Result.ok();
    }

    @PostMapping("/api/replies/{id}/report")
    public Result<Void> reportReply(@PathVariable("id") Long id,
                                    @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                    @Valid @RequestBody ReportCreateRequest request) {
        postService.reportReply(id, userId, request);
        return Result.ok();
    }

    @GetMapping("/api/banned-words")
    public Result<List<BannedWordVO>> bannedWords() {
        return Result.ok(postService.listBannedWords());
    }

    @PostMapping("/api/banned-words")
    public Result<BannedWordVO> addBannedWord(@RequestBody BannedWordVO request) {
        return Result.ok(postService.addBannedWord(request.getWord()));
    }

    @DeleteMapping("/api/banned-words/{id}")
    public Result<Void> deleteBannedWord(@PathVariable("id") Long id) {
        postService.deleteBannedWord(id);
        return Result.ok();
    }

    @GetMapping("/api/posts/by-author/{authorId}")
    public Result<PageResult<PostVO>> postsByAuthor(@PathVariable Long authorId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestHeader(value = ForumConstants.HEADER_USER_ID, required = false) Long viewerId) {
        return Result.ok(postService.listPublicPostsByAuthor(authorId, page, size, viewerId));
    }

    @GetMapping("/api/posts/favorites/by-user/{userId}")
    public Result<PageResult<PostVO>> favoritesByUser(@PathVariable Long userId,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestHeader(value = ForumConstants.HEADER_USER_ID, required = false) Long viewerId) {
        return Result.ok(postService.listPublicFavoritesByUser(userId, page, size, viewerId));
    }

    @GetMapping("/api/replies/by-author/{authorId}")
    public Result<List<ReplyVO>> repliesByAuthor(@PathVariable Long authorId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestHeader(value = ForumConstants.HEADER_USER_ID, required = false) Long viewerId) {
        return Result.ok(postService.listPublicRepliesByAuthor(authorId, page, size, viewerId));
    }

    @GetMapping("/api/reports")
    public Result<PageResult<ReportVO>> listReports(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return Result.ok(postService.listPendingReports(page, size));
    }

    @PutMapping("/api/reports/{id}/handle")
    public Result<Void> handleReport(@PathVariable("id") Long id, @RequestParam String action) {
        postService.handleReport(id, action);
        return Result.ok();
    }

    @PostMapping("/api/posts/{id}/profile-pin")
    public Result<Void> pinProfilePost(@PathVariable("id") Long id,
                                       @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        postService.pinProfilePost(id, userId);
        return Result.ok();
    }

    @DeleteMapping("/api/posts/{id}/profile-pin")
    public Result<Void> unpinProfilePost(@PathVariable("id") Long id,
                                         @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        postService.unpinProfilePost(id, userId);
        return Result.ok();
    }

    @PostMapping("/api/posts/{id}/official-pin")
    public Result<Void> pinOfficialPost(@PathVariable("id") Long id,
                                        @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.pinOfficialPost(id, role);
        return Result.ok();
    }

    @DeleteMapping("/api/posts/{id}/official-pin")
    public Result<Void> unpinOfficialPost(@PathVariable("id") Long id,
                                          @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        postService.unpinOfficialPost(id, role);
        return Result.ok();
    }

    @PostMapping("/api/posts/{postId}/pin-reply/{replyId}")
    public Result<Void> pinReply(@PathVariable Long postId,
                                 @PathVariable Long replyId,
                                 @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        postService.pinReply(postId, replyId, userId);
        return Result.ok();
    }

    @DeleteMapping("/api/posts/{postId}/pin-reply")
    public Result<Void> unpinReply(@PathVariable Long postId,
                                   @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        postService.unpinReply(postId, userId);
        return Result.ok();
    }
}
