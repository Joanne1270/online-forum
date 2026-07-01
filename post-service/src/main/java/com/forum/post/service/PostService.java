package com.forum.post.service;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.ForumMessage;
import com.forum.common.dto.NotificationCreateRequest;
import com.forum.common.dto.PageResult;
import com.forum.common.dto.Result;
import com.forum.common.exception.BusinessException;
import com.forum.common.util.TextSimilarity;
import com.forum.post.client.NotificationClient;
import com.forum.post.client.UserClient;
import com.forum.post.config.PostRedisObjectMapper;
import com.forum.post.dto.*;
import com.forum.post.entity.Board;
import com.forum.post.entity.BannedWord;
import com.forum.post.entity.Post;
import com.forum.post.entity.PostReport;
import com.forum.post.entity.Reply;
import com.forum.post.entity.ReplyReport;
import com.forum.post.mapper.*;
import com.forum.post.mq.MessageProducer;
import com.forum.post.util.PostContentNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final Pattern MENTION_ID_PATTERN = Pattern.compile("@\\[(\\d+)\\][^\\s@]*");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(?!#)([^\\s@\\[]{1,50})");
    private static final Duration POST_CACHE_TTL = Duration.ofMinutes(10);

    private final BoardMapper boardMapper;
    private final PostMapper postMapper;
    private final ReplyMapper replyMapper;
    private final LikeMapper likeMapper;
    private final FavoriteMapper favoriteMapper;
    private final ReportMapper reportMapper;
    private final ReplyReportMapper replyReportMapper;
    private final MentionMapper mentionMapper;
    private final UserProfilePinMapper userProfilePinMapper;
    private final MessageProducer messageProducer;
    private final NotificationClient notificationClient;
    private final UserClient userClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRedisObjectMapper postRedisObjectMapper;
    private final BannedWordService bannedWordService;
    private final TagService tagService;

    public List<BoardVO> listBoards() {
        List<Board> all = boardMapper.findAll();
        Map<Long, BoardVO> map = new LinkedHashMap<>();
        for (Board board : all) {
            map.put(board.getId(), toBoardVO(board));
        }
        List<BoardVO> roots = new ArrayList<>();
        for (Board board : all) {
            BoardVO vo = map.get(board.getId());
            if (board.getParentId() == null) {
                roots.add(vo);
            } else {
                BoardVO parent = map.get(board.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    public Board createBoard(Board board) {
        if (board.getParentId() != null) {
            Board parent = boardMapper.findById(board.getParentId());
            if (parent == null || !ForumConstants.BOARD_TYPE_CATEGORY.equals(parent.getBoardType())) {
                throw new BusinessException("只能在大版块下创建小版块");
            }
            board.setBoardType(ForumConstants.BOARD_TYPE_NORMAL);
        } else {
            if (board.getBoardType() == null) {
                board.setBoardType(ForumConstants.BOARD_TYPE_CATEGORY);
            }
            if (!ForumConstants.BOARD_TYPE_CATEGORY.equals(board.getBoardType())) {
                throw new BusinessException("仅可新增大版块，系统版块请通过数据库维护");
            }
        }
        boardMapper.insert(board);
        return board;
    }

    public Board updateBoard(Board board) {
        Board existing = boardMapper.findById(board.getId());
        if (existing == null) {
            throw new BusinessException("版块不存在");
        }
        board.setBoardType(existing.getBoardType());
        board.setParentId(existing.getParentId());
        boardMapper.update(board);
        return boardMapper.findById(board.getId());
    }

    public void deleteBoard(Long id) {
        Board board = boardMapper.findById(id);
        if (board != null && (ForumConstants.BOARD_TYPE_ALL.equals(board.getBoardType())
                || ForumConstants.BOARD_TYPE_HOME.equals(board.getBoardType()))) {
            throw new BusinessException("系统版块不可删除");
        }
        if (board != null && boardMapper.countByParentId(id) > 0) {
            throw new BusinessException("请先删除该版块下的小版块");
        }
        if (board != null && ForumConstants.BOARD_TYPE_NORMAL.equals(board.getBoardType())
                && postMapper.countByBoard(id) > 0) {
            throw new BusinessException("该小版块下仍有帖子，无法删除");
        }
        boardMapper.delete(id);
    }

    @Transactional
    public PostVO createPost(Long userId, String nickname, String role, PostCreateRequest request) {
        requirePostableBoard(request.getBoardId(), role);
        String content = normalizePostContent(request.getBoardId(), request.getContent());
        validatePostContent(request.getBoardId(), content);
        assertNotSimilarRecentPost(userId, role, request.getTitle(), content, null, request.getConfirmSimilar());
        String rateKey = ForumConstants.REDIS_RATE_POST + userId;
        Boolean allowed = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(allowed)) {
            throw new BusinessException("发帖过于频繁，请稍后再试");
        }
        bannedWordService.assertClean(request.getTitle() + " " + content);
        Post post = new Post();
        post.setBoardId(request.getBoardId());
        post.setAuthorId(userId);
        post.setTitle(request.getTitle());
        post.setContent(content);
        postMapper.insert(post);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setReplyCount(0);
        post.setStatus(1);
        updateHotScore(post);
        String authorName = resolveAuthorName(userId);
        processMentions(userId, authorName, post.getId(), null, content, "POST");
        tagService.syncPostTags(post.getId(), content);
        ForumMessage msg = new ForumMessage();
        msg.setType(ForumConstants.TAG_POST_CREATED);
        msg.setFromUserId(userId);
        msg.setFromNickname(authorName);
        msg.setRefId(post.getId());
        msg.setRefType("POST");
        msg.setContent(authorName + " 发布了新帖子：" + post.getTitle());
        messageProducer.send(ForumConstants.TAG_POST_CREATED, msg);
        return enrichPost(post, userId);
    }

    @Transactional
    public PostVO createDraft(Long userId, String role, PostDraftRequest request) {
        if (request.getBoardId() == null) {
            throw new BusinessException("请选择版块");
        }
        requirePostableBoard(request.getBoardId(), role);
        Post post = new Post();
        post.setBoardId(request.getBoardId());
        post.setAuthorId(userId);
        post.setTitle(trimToEmpty(request.getTitle()));
        post.setContent(PostContentNormalizer.trimPostContent(request.getContent()));
        postMapper.insertDraft(post);
        return enrichPost(postMapper.findDraftById(post.getId(), userId), userId);
    }

    @Transactional
    public PostVO updateDraft(Long id, Long userId, String role, PostDraftRequest request) {
        Post post = postMapper.findDraftById(id, userId);
        if (post == null) {
            throw new BusinessException("草稿不存在");
        }
        if (request.getBoardId() != null) {
            requirePostableBoard(request.getBoardId(), role);
            post.setBoardId(request.getBoardId());
        }
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            post.setContent(PostContentNormalizer.trimPostContent(request.getContent()));
        }
        postMapper.updateDraft(post);
        return enrichPost(postMapper.findDraftById(id, userId), userId);
    }

    public PostVO getDraftDetail(Long id, Long userId) {
        Post post = postMapper.findDraftById(id, userId);
        if (post == null) {
            throw new BusinessException("草稿不存在");
        }
        return enrichPost(post, userId);
    }

    public PageResult<PostVO> listDrafts(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Post> posts = postMapper.listDraftsByAuthor(userId, offset, size);
        List<PostVO> list = posts.stream().map(p -> enrichPost(p, userId)).collect(Collectors.toList());
        return new PageResult<>(list, postMapper.countDraftsByAuthor(userId), page, size);
    }

    @Transactional
    public PostVO publishDraft(Long id, Long userId, String nickname, String role, PostDraftRequest request) {
        Post post = postMapper.findDraftById(id, userId);
        if (post == null) {
            throw new BusinessException("草稿不存在");
        }
        Long boardId = request != null && request.getBoardId() != null ? request.getBoardId() : post.getBoardId();
        String title = request != null && request.getTitle() != null ? request.getTitle().trim() : trimToEmpty(post.getTitle());
        String rawContent = request != null && request.getContent() != null ? request.getContent() : post.getContent();
        requirePostableBoard(boardId, role);
        String content = normalizePostContent(boardId, rawContent);
        validatePostContent(boardId, content);
        if (title.isBlank()) {
            throw new BusinessException("标题不能为空");
        }
        assertNotSimilarRecentPost(userId, role, title, content, id, request != null ? request.getConfirmSimilar() : null);
        String rateKey = ForumConstants.REDIS_RATE_POST + userId;
        Boolean allowed = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(allowed)) {
            throw new BusinessException("发帖过于频繁，请稍后再试");
        }
        bannedWordService.assertClean(title + " " + content);
        post.setBoardId(boardId);
        post.setTitle(title);
        post.setContent(content);
        if (postMapper.publishDraft(post) == 0) {
            throw new BusinessException("发布失败");
        }
        post.setStatus(ForumConstants.POST_STATUS_NORMAL);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setReplyCount(0);
        updateHotScore(post);
        String authorName = resolveAuthorName(userId);
        processMentions(userId, authorName, post.getId(), null, content, "POST");
        tagService.syncPostTags(post.getId(), content);
        ForumMessage msg = new ForumMessage();
        msg.setType(ForumConstants.TAG_POST_CREATED);
        msg.setFromUserId(userId);
        msg.setFromNickname(authorName);
        msg.setRefId(post.getId());
        msg.setRefType("POST");
        msg.setContent(authorName + " 发布了新帖子：" + title);
        messageProducer.send(ForumConstants.TAG_POST_CREATED, msg);
        return enrichPost(postMapper.findById(id), userId);
    }

    public void deleteDraft(Long id, Long userId) {
        Post post = postMapper.findDraftById(id, userId);
        if (post == null) {
            throw new BusinessException("草稿不存在");
        }
        postMapper.softDelete(id);
    }

    public PostSimilarCheckVO checkSimilarPost(Long userId, String role, PostSimilarCheckRequest request) {
        PostSimilarCheckVO vo = new PostSimilarCheckVO();
        vo.setSimilar(isSimilarToRecentPost(userId, role,
                request != null ? request.getTitle() : null,
                request != null ? request.getContent() : null,
                request != null ? request.getExcludePostId() : null));
        return vo;
    }

    public PageResult<PostVO> listPosts(Long boardId, int page, int size, Long currentUserId, String sort, Boolean featuredOnly) {
        int offset = (page - 1) * size;
        List<Post> posts;
        long total;
        if (boardId == null || isAllBoard(boardId)) {
            if (Boolean.TRUE.equals(featuredOnly)) {
                posts = postMapper.listAllFeatured(offset, size);
                total = postMapper.countAllFeatured();
            } else {
                posts = postMapper.listAll(offset, size);
                total = postMapper.countAll();
            }
        } else if (isHomeRootBoard(boardId)) {
            List<Long> childIds = boardMapper.findIdsByParentId(boardId);
            if (childIds.isEmpty()) {
                posts = listBoardWithSort(boardId, offset, size, sort, true);
                total = postMapper.countByBoard(boardId);
            } else {
                posts = listBoardIdsWithSort(childIds, offset, size, sort, true);
                total = postMapper.countByBoardIds(childIds);
            }
        } else if (isCategoryBoard(boardId)) {
            List<Long> childIds = boardMapper.findIdsByParentId(boardId);
            if (childIds.isEmpty()) {
                posts = Collections.emptyList();
                total = 0;
            } else {
                posts = listBoardIdsWithSort(childIds, offset, size, sort, false);
                total = postMapper.countByBoardIds(childIds);
            }
        } else {
            posts = listBoardWithSort(boardId, offset, size, sort, false);
            total = postMapper.countByBoard(boardId);
        }
        List<PostVO> list = posts.stream().map(p -> enrichPost(p, currentUserId)).collect(Collectors.toList());
        return new PageResult<>(list, total, page, size);
    }

    public PageResult<PostVO> searchPosts(String keyword, int page, int size, Long currentUserId) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, page, size);
        }
        int offset = (page - 1) * size;
        List<Post> posts;
        long total;
        if (normalized.startsWith("#")) {
            String tagName = tagService.normalizeTagName(normalized.substring(1));
            if (tagName.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0, page, size);
            }
            posts = postMapper.searchByTag(tagName, offset, size);
            total = postMapper.countSearchByTag(tagName);
        } else {
            String searchKeyword = tagService.normalizeTagName(normalized);
            if (searchKeyword.isEmpty()) {
                searchKeyword = normalized;
            }
            posts = postMapper.search(searchKeyword, offset, size);
            total = postMapper.countSearch(searchKeyword);
        }
        List<PostVO> list = posts.stream().map(p -> enrichPost(p, currentUserId)).collect(Collectors.toList());
        return new PageResult<>(list, total, page, size);
    }

    public PageResult<PostVO> listFollowingFeed(Long userId, String role, int page, int size) {
        if (ForumConstants.ROLE_ADMIN.equals(role)) {
            return new PageResult<>(Collections.emptyList(), 0, page, size);
        }
        Result<List<Long>> followResult = userClient.followingIds(userId, role);
        List<Long> authorIds = followResult != null && followResult.getData() != null
                ? followResult.getData() : Collections.emptyList();
        if (authorIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, page, size);
        }
        int offset = (page - 1) * size;
        List<Post> posts = postMapper.listByAuthors(authorIds, offset, size);
        long total = postMapper.countByAuthors(authorIds);
        List<PostVO> list = posts.stream().map(p -> enrichPost(p, userId)).collect(Collectors.toList());
        return new PageResult<>(list, total, page, size);
    }

    public PostVO getPostDetail(Long id, Long currentUserId) {
        String cacheKey = ForumConstants.REDIS_POST_DETAIL + id;
        Post post = readCachedPost(cacheKey);
        if (post == null) {
            post = postMapper.findById(id);
            if (post == null) {
                throw new BusinessException("帖子不存在");
            }
            redisTemplate.opsForValue().set(cacheKey, post, POST_CACHE_TTL);
        }
        postMapper.incrementView(id);
        post.setViewCount(countOrZero(post.getViewCount()) + 1);
        updateHotScore(post);
        refreshFeaturedStatus(postMapper.findById(id));
        Post fresh = postMapper.findById(id);
        tagService.syncPostTagsIfMismatch(fresh.getId(), fresh.getContent());
        return enrichPost(fresh, currentUserId);
    }

    @Transactional
    public PostVO updatePost(Long id, Long userId, PostUpdateRequest request) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw new BusinessException("无权编辑该帖子");
        }
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            post.setContent(PostContentNormalizer.trimPostContent(request.getContent()));
        }
        bannedWordService.assertClean(post.getTitle() + " " + post.getContent());
        postMapper.update(post);
        if (request.getContent() != null) {
            syncPostMentions(userId, resolveAuthorName(userId), post.getId(), post.getContent());
            tagService.syncPostTags(post.getId(), post.getContent());
        }
        invalidatePostCache(id);
        return enrichPost(postMapper.findById(id), userId);
    }

    public void deletePost(Long id, Long userId, boolean isAdmin) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!isAdmin && !post.getAuthorId().equals(userId)) {
            throw new BusinessException("无权删除该帖子");
        }
        postMapper.softDelete(id);
        replyMapper.softDeleteByPostId(id);
        userProfilePinMapper.deleteByPostId(id);
        tagService.removePostTags(id);
        invalidatePostCache(id);
        if (isAdmin && !post.getAuthorId().equals(userId)) {
            deliverSystemNotification(post.getAuthorId(), ForumConstants.TAG_POST_DELETED, post.getId(), "POST", post.getId(),
                    "您的帖子《" + post.getTitle() + "》已被管理员删除");
        }
    }

    @Transactional
    public ReplyVO createReply(Long postId, Long userId, String nickname, String role, ReplyCreateRequest request) {
        denyAdminInteraction(role);
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        bannedWordService.assertClean(request.getContent());
        Reply reply = new Reply();
        reply.setPostId(postId);
        reply.setParentId(request.getParentId());
        reply.setAuthorId(userId);
        reply.setContent(request.getContent().trim());
        replyMapper.insert(reply);
        postMapper.updateReplyCount(postId, 1);
        processMentions(userId, resolveAuthorName(userId), postId, reply.getId(), request.getContent(), "REPLY");
        String authorName = resolveAuthorName(userId);
        ForumMessage replyMsg = new ForumMessage();
        replyMsg.setType(ForumConstants.TAG_REPLY_CREATED);
        replyMsg.setFromUserId(userId);
        replyMsg.setFromNickname(authorName);
        replyMsg.setToUserId(post.getAuthorId());
        replyMsg.setRefId(reply.getId());
        replyMsg.setRefType("REPLY");
        replyMsg.setPostId(postId);
        replyMsg.setContent(authorName + " 回复了你的帖子：" + truncateNotificationText(request.getContent()));
        messageProducer.send(ForumConstants.TAG_REPLY_CREATED, replyMsg);
        deliverNotification(replyMsg);
        invalidatePostCache(postId);
        Post refreshed = postMapper.findById(postId);
        if (refreshed != null) {
            updateHotScore(refreshed);
            refreshFeaturedStatus(refreshed);
        }
        ReplyVO vo = ReplyVO.from(reply);
        vo.setAuthorName(resolveAuthorName(userId));
        return vo;
    }

    public List<ReplyVO> listReplies(Long postId, Long currentUserId, String sort) {
        Post post = postMapper.findById(postId);
        Long pinnedReplyId = post != null ? post.getPinnedReplyId() : null;
        List<Reply> flat = replyMapper.listByPostId(postId);
        Map<Long, Reply> map = new LinkedHashMap<>();
        flat.forEach(r -> map.put(r.getId(), r));
        List<Reply> roots = new ArrayList<>();
        for (Reply reply : flat) {
            if (reply.getParentId() == null) {
                roots.add(reply);
            } else {
                Reply parent = map.get(reply.getParentId());
                if (parent != null) {
                    parent.getChildren().add(reply);
                } else {
                    roots.add(reply);
                }
            }
        }
        List<ReplyVO> result = roots.stream().map(r -> enrichReply(r, currentUserId, pinnedReplyId)).collect(Collectors.toList());
        sortReplyTree(result, sort, currentUserId, true, pinnedReplyId);
        return result;
    }

    @Transactional
    public void likePost(Long postId, Long userId, String nickname, String role) {
        denyAdminInteraction(role);
        if (likeMapper.existsPostLike(postId, userId) > 0) {
            throw new BusinessException("已经点赞过了");
        }
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        likeMapper.insertPostLike(postId, userId);
        postMapper.updateLikeCount(postId, 1);
        post.setLikeCount(countOrZero(post.getLikeCount()) + 1);
        updateHotScore(post);
        invalidatePostCache(postId);
        refreshFeaturedStatus(postMapper.findById(postId));
        sendLikeMessage(userId, post.getAuthorId(), postId, "POST", postId,
                "点赞了你的帖子：" + post.getTitle());
    }

    @Transactional
    public void unlikePost(Long postId, Long userId, String role) {
        denyAdminInteraction(role);
        if (likeMapper.deletePostLike(postId, userId) > 0) {
            postMapper.updateLikeCount(postId, -1);
            Post post = postMapper.findById(postId);
            if (post != null) {
                updateHotScore(post);
                refreshFeaturedStatus(post);
            }
            invalidatePostCache(postId);
        }
    }

    @Transactional
    public void likeReply(Long replyId, Long userId, String nickname, String role) {
        denyAdminInteraction(role);
        if (likeMapper.existsReplyLike(replyId, userId) > 0) {
            throw new BusinessException("已经点赞过了");
        }
        Reply reply = replyMapper.findById(replyId);
        if (reply == null) {
            throw new BusinessException("回复不存在");
        }
        likeMapper.insertReplyLike(replyId, userId);
        replyMapper.updateLikeCount(replyId, 1);
        if (likeMapper.existsReplyDislike(replyId, userId) > 0) {
            likeMapper.deleteReplyDislike(replyId, userId);
            replyMapper.updateDislikeCount(replyId, -1);
        }
        sendLikeMessage(userId, reply.getAuthorId(), replyId, "REPLY", reply.getPostId(),
                "点赞了你的回复：" + truncateNotificationText(reply.getContent()));
    }

    @Transactional
    public void unlikeReply(Long replyId, Long userId, String role) {
        denyAdminInteraction(role);
        if (likeMapper.deleteReplyLike(replyId, userId) > 0) {
            replyMapper.updateLikeCount(replyId, -1);
        }
    }

    @Transactional
    public void dislikeReply(Long replyId, Long userId, String role) {
        denyAdminInteraction(role);
        if (likeMapper.existsReplyDislike(replyId, userId) > 0) {
            throw new BusinessException("已经拉踩过了");
        }
        Reply reply = replyMapper.findById(replyId);
        if (reply == null) {
            throw new BusinessException("回复不存在");
        }
        boolean removedLike = likeMapper.existsReplyLike(replyId, userId) > 0;
        if (removedLike) {
            likeMapper.deleteReplyLike(replyId, userId);
            replyMapper.updateLikeCount(replyId, -1);
        }
        likeMapper.insertReplyDislike(replyId, userId);
        replyMapper.updateDislikeCount(replyId, 1);
        maybeAutoModerateReply(reply, removedLike);
    }

    @Transactional
    public void undislikeReply(Long replyId, Long userId, String role) {
        denyAdminInteraction(role);
        if (likeMapper.deleteReplyDislike(replyId, userId) > 0) {
            replyMapper.updateDislikeCount(replyId, -1);
        }
    }

    @Transactional
    public void favoritePost(Long postId, Long userId, String nickname, String role) {
        denyAdminInteraction(role);
        if (favoriteMapper.exists(postId, userId) > 0) {
            throw new BusinessException("已经收藏过了");
        }
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        favoriteMapper.insert(postId, userId);
        String authorName = resolveAuthorName(userId);
        ForumMessage msg = new ForumMessage();
        msg.setType(ForumConstants.TAG_FAVORITE);
        msg.setFromUserId(userId);
        msg.setFromNickname(authorName);
        msg.setToUserId(post.getAuthorId());
        msg.setRefId(postId);
        msg.setRefType("POST");
        msg.setPostId(postId);
        msg.setContent(authorName + " 收藏了你的帖子：" + post.getTitle());
        deliverNotification(msg);
        refreshFeaturedStatus(postMapper.findById(postId));
    }

    @Transactional
    public void unfavoritePost(Long postId, Long userId, String role) {
        denyAdminInteraction(role);
        favoriteMapper.delete(postId, userId);
        refreshFeaturedStatus(postMapper.findById(postId));
    }

    public PageResult<PostVO> listFavorites(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Post> posts = favoriteMapper.listByUser(userId, offset, size);
        long total = favoriteMapper.countByUser(userId);
        List<PostVO> list = posts.stream().map(p -> enrichPost(p, userId)).collect(Collectors.toList());
        return new PageResult<>(list, total, page, size);
    }

    @Transactional
    public void reportPost(Long postId, Long userId, ReportCreateRequest request) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (post.getAuthorId().equals(userId)) {
            throw new BusinessException("不能举报自己的帖子");
        }
        if (reportMapper.exists(postId, userId) > 0) {
            throw new BusinessException("您已举报过该帖子");
        }
        PostReport report = new PostReport();
        report.setPostId(postId);
        report.setReporterId(userId);
        report.setReason(request.getReason());
        reportMapper.insert(report);
    }

    @Transactional
    public void deleteReply(Long replyId, Long userId) {
        Reply reply = replyMapper.findById(replyId);
        if (reply == null) {
            throw new BusinessException("回复不存在");
        }
        if (!reply.getAuthorId().equals(userId)) {
            throw new BusinessException("无权删除该回复");
        }
        replyMapper.softDelete(replyId);
        postMapper.updateReplyCount(reply.getPostId(), -1);
        Post post = postMapper.findById(reply.getPostId());
        if (post != null && replyId.equals(post.getPinnedReplyId())) {
            postMapper.updatePinnedReply(reply.getPostId(), null);
        }
        if (post != null) {
            refreshFeaturedStatus(postMapper.findById(reply.getPostId()));
        }
        invalidatePostCache(reply.getPostId());
    }

    @Transactional
    public void reportReply(Long replyId, Long userId, ReportCreateRequest request) {
        Reply reply = replyMapper.findById(replyId);
        if (reply == null) {
            throw new BusinessException("回复不存在");
        }
        if (reply.getAuthorId().equals(userId)) {
            throw new BusinessException("不能举报自己的回复");
        }
        if (replyReportMapper.exists(replyId, userId) > 0) {
            throw new BusinessException("您已举报过该回复");
        }
        ReplyReport report = new ReplyReport();
        report.setReplyId(replyId);
        report.setReporterId(userId);
        report.setReason(request.getReason());
        replyReportMapper.insert(report);
    }

    public boolean postExists(Long id) {
        return postMapper.findById(id) != null;
    }

    public PageResult<ReportVO> listPendingReports(int page, int size) {
        int offset = (page - 1) * size;
        List<ReportVO> list = reportMapper.listPending(offset, size);
        list.forEach(r -> r.setReporterName(resolveAuthorName(r.getReporterId())));
        long total = reportMapper.countPending();
        return new PageResult<>(list, total, page, size);
    }

    @Transactional
    public void handleReport(Long reportId, String action) {
        PostReport report = reportMapper.findById(reportId);
        if (report == null || report.getStatus() != ForumConstants.REPORT_STATUS_PENDING) {
            throw new BusinessException("举报记录不存在或已处理");
        }
        Post post = postMapper.findById(report.getPostId());
        String postTitle = post != null ? post.getTitle() : "相关帖子";
        if ("DELETE_POST".equals(action)) {
            if (post != null) {
                postMapper.softDelete(report.getPostId());
                replyMapper.softDeleteByPostId(report.getPostId());
                invalidatePostCache(report.getPostId());
                deliverSystemNotification(post.getAuthorId(), ForumConstants.TAG_POST_DELETED, post.getId(), "POST", post.getId(),
                        "您的帖子《" + postTitle + "》因被举报核实已被删除");
            }
            reportMapper.updateStatus(reportId, ForumConstants.REPORT_STATUS_HANDLED);
            deliverSystemNotification(report.getReporterId(), ForumConstants.TAG_REPORT_RESULT, report.getPostId(), "POST", report.getPostId(),
                    "您举报的帖子《" + postTitle + "》已处理：帖子已删除");
        } else if ("DISMISS".equals(action)) {
            reportMapper.updateStatus(reportId, ForumConstants.REPORT_STATUS_DISMISSED);
            deliverSystemNotification(report.getReporterId(), ForumConstants.TAG_REPORT_RESULT, report.getPostId(), "POST", report.getPostId(),
                    "您举报的帖子《" + postTitle + "》已处理：经核查暂未违规");
        } else {
            throw new BusinessException("无效的处理方式");
        }
    }

    public List<PostVO> hotPosts(Long boardId, int limit, Long currentUserId) {
        String key = ForumConstants.REDIS_HOT_POSTS + (boardId == null || isAllBoard(boardId) ? "all" : boardId);
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, limit - 1);
        if (tuples == null || tuples.isEmpty()) {
            if (boardId == null || isAllBoard(boardId)) {
                List<Post> posts = postMapper.listAllByHot(0, limit);
                return posts.stream().map(p -> enrichPost(p, currentUserId)).collect(Collectors.toList());
            }
            PageResult<PostVO> page = listPosts(boardId, 1, limit, currentUserId, "hot", false);
            return page.getList();
        }
        List<PostVO> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long postId = Long.parseLong(String.valueOf(tuple.getValue()));
            Post post = postMapper.findById(postId);
            if (post != null) {
                result.add(enrichPost(post, currentUserId));
            }
        }
        return result;
    }

    public PageResult<PostVO> myPosts(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Post> posts = postMapper.listByAuthor(userId, offset, size);
        List<PostVO> list = posts.stream().map(p -> enrichPost(p, userId)).collect(Collectors.toList());
        applyProfilePinFlags(list, userId);
        long total = postMapper.countByAuthor(userId);
        return new PageResult<>(list, total, page, size);
    }

    public PageResult<PostVO> listMyLikes(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Post> posts = likeMapper.listLikedPosts(userId, offset, size);
        long total = likeMapper.countLikedPosts(userId);
        List<PostVO> list = posts.stream().map(p -> enrichPost(p, userId)).collect(Collectors.toList());
        return new PageResult<>(list, total, page, size);
    }

    public PageResult<UserReportVO> listMyReports(Long userId, int page, int size) {
        List<UserReportVO> combined = new ArrayList<>();
        combined.addAll(reportMapper.listByReporter(userId, 0, 500));
        combined.addAll(replyReportMapper.listByReporter(userId, 0, 500));
        combined.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                return 0;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        long total = combined.size();
        int offset = (page - 1) * size;
        List<UserReportVO> list = combined.stream().skip(offset).limit(size).collect(Collectors.toList());
        return new PageResult<>(list, total, page, size);
    }

    public List<ReplyVO> myReplies(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        return replyMapper.listByAuthor(userId, offset, size).stream()
                .map(r -> enrichReply(r, userId)).collect(Collectors.toList());
    }

    public Long getReplyPostId(Long replyId) {
        Long postId = replyMapper.findPostIdById(replyId);
        if (postId == null) {
            throw new BusinessException("回复不存在");
        }
        return postId;
    }

    public String getReplyPreview(Long replyId) {
        Reply reply = replyMapper.findById(replyId);
        if (reply == null) {
            throw new BusinessException("回复不存在");
        }
        return truncateNotificationText(reply.getContent());
    }

    public List<BannedWordVO> listBannedWords() {
        return bannedWordService.listAll().stream().map(word -> {
            BannedWordVO vo = new BannedWordVO();
            vo.setId(word.getId());
            vo.setWord(word.getWord());
            vo.setEnabled(word.getEnabled());
            return vo;
        }).collect(Collectors.toList());
    }

    public BannedWordVO addBannedWord(String word) {
        BannedWord bannedWord = bannedWordService.addWord(word);
        BannedWordVO vo = new BannedWordVO();
        vo.setId(bannedWord.getId());
        vo.setWord(bannedWord.getWord());
        vo.setEnabled(1);
        return vo;
    }

    public void deleteBannedWord(Long id) {
        bannedWordService.deleteWord(id);
    }

    public PageResult<PostVO> listPublicPostsByAuthor(Long authorId, int page, int size, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Post> posts = postMapper.listPublicByAuthor(authorId, offset, size);
        List<PostVO> list = posts.stream().map(p -> enrichPost(p, currentUserId)).collect(Collectors.toList());
        applyProfilePinFlags(list, authorId);
        return new PageResult<>(list, postMapper.countPublicByAuthor(authorId), page, size);
    }

    public PageResult<PostVO> listPublicFavoritesByUser(Long userId, int page, int size, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Post> posts = favoriteMapper.listByUser(userId, offset, size);
        long total = favoriteMapper.countByUser(userId);
        List<PostVO> list = posts.stream().map(p -> enrichPost(p, currentUserId)).collect(Collectors.toList());
        return new PageResult<>(list, total, page, size);
    }

    public List<ReplyVO> listPublicRepliesByAuthor(Long authorId, int page, int size, Long currentUserId) {
        int offset = (page - 1) * size;
        return replyMapper.listByAuthor(authorId, offset, size).stream()
                .map(r -> enrichReply(r, currentUserId, null)).collect(Collectors.toList());
    }

    @Transactional
    public void pinProfilePost(Long postId, Long userId) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw new BusinessException("只能置顶自己的帖子");
        }
        if (userProfilePinMapper.exists(userId, postId) > 0) {
            throw new BusinessException("该帖子已置顶");
        }
        if (userProfilePinMapper.countByUser(userId) >= ForumConstants.MAX_PROFILE_PINS) {
            throw new BusinessException("最多置顶 " + ForumConstants.MAX_PROFILE_PINS + " 篇帖子");
        }
        userProfilePinMapper.insert(userId, postId, userProfilePinMapper.maxSortOrder(userId) + 1);
    }

    @Transactional
    public void unpinProfilePost(Long postId, Long userId) {
        if (userProfilePinMapper.delete(userId, postId) == 0) {
            throw new BusinessException("该帖子未置顶");
        }
    }

    @Transactional
    public void pinOfficialPost(Long postId, String role) {
        if (!ForumConstants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException("仅管理员可操作");
        }
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!isOfficialBoard(post.getBoardId())) {
            throw new BusinessException("仅可置顶官方公告帖子");
        }
        postMapper.updateOfficialPinned(postId, 1, LocalDateTime.now());
        invalidatePostCache(postId);
    }

    @Transactional
    public void unpinOfficialPost(Long postId, String role) {
        if (!ForumConstants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException("仅管理员可操作");
        }
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        postMapper.updateOfficialPinned(postId, 0, null);
        invalidatePostCache(postId);
    }

    @Transactional
    public void pinReply(Long postId, Long replyId, Long userId) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw new BusinessException("仅楼主可置顶回复");
        }
        Reply reply = replyMapper.findById(replyId);
        if (reply == null || !reply.getPostId().equals(postId)) {
            throw new BusinessException("回复不存在");
        }
        if (reply.getParentId() != null) {
            throw new BusinessException("仅可置顶一级回复");
        }
        postMapper.updatePinnedReply(postId, replyId);
        invalidatePostCache(postId);
    }

    @Transactional
    public void unpinReply(Long postId, Long userId) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!post.getAuthorId().equals(userId)) {
            throw new BusinessException("仅楼主可取消置顶");
        }
        postMapper.updatePinnedReply(postId, null);
        invalidatePostCache(postId);
    }

    private void denyAdminInteraction(String role) {
        if (ForumConstants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException("管理员无法进行此操作");
        }
    }

    private boolean isHomeRootBoard(Long boardId) {
        Board board = boardMapper.findById(boardId);
        return board != null && ForumConstants.BOARD_TYPE_HOME.equals(board.getBoardType());
    }

    private boolean isHomeChildBoard(Board board) {
        if (board == null || board.getParentId() == null
                || !ForumConstants.BOARD_TYPE_NORMAL.equals(board.getBoardType())) {
            return false;
        }
        Board parent = boardMapper.findById(board.getParentId());
        return parent != null && ForumConstants.BOARD_TYPE_HOME.equals(parent.getBoardType());
    }

    private boolean isOfficialBoard(Long boardId) {
        Board board = boardMapper.findById(boardId);
        if (board == null) {
            return false;
        }
        if (ForumConstants.BOARD_TYPE_HOME.equals(board.getBoardType())) {
            return true;
        }
        return isHomeChildBoard(board);
    }

    private void applyAuthorDisplay(PostVO vo, Post post) {
        if (isOfficialBoard(post.getBoardId())) {
            vo.setOfficial(true);
            vo.setAuthorName(ForumConstants.OFFICIAL_AUTHOR_NAME);
            vo.setAuthorId(null);
        } else {
            vo.setOfficial(false);
            vo.setAuthorName(resolveAuthorName(post.getAuthorId()));
        }
    }

    public long countPosts() {
        return postMapper.countAll();
    }

    private void requirePostableBoard(Long boardId, String role) {
        Board board = boardMapper.findById(boardId);
        if (board == null) {
            throw new BusinessException("版块不存在");
        }
        if (ForumConstants.ROLE_ADMIN.equals(role)) {
            if (ForumConstants.BOARD_TYPE_HOME.equals(board.getBoardType()) || isHomeChildBoard(board)) {
                return;
            }
            throw new BusinessException("管理员仅可在首页版块发布官方公告");
        }
        if (ForumConstants.BOARD_TYPE_ALL.equals(board.getBoardType())
                || ForumConstants.BOARD_TYPE_CATEGORY.equals(board.getBoardType())) {
            throw new BusinessException("请选择具体的小版块发帖");
        }
        if (ForumConstants.BOARD_TYPE_HOME.equals(board.getBoardType())) {
            throw new BusinessException("请选择首页下的小版块");
        }
        if (isHomeChildBoard(board)) {
            throw new BusinessException("官方公告版块仅管理员可发布");
        }
        if (ForumConstants.BOARD_TYPE_NORMAL.equals(board.getBoardType()) && board.getParentId() == null) {
            if (!isStandalonePostableBoard(board)) {
                throw new BusinessException("请选择具体的小版块发帖");
            }
        }
    }

    private boolean isStandalonePostableBoard(Board board) {
        return board != null && ForumConstants.BOARD_INTERACTIVE_QA.equals(board.getName());
    }

    private List<Post> listBoardWithSort(Long boardId, int offset, int size, String sort, boolean official) {
        if (official) {
            return postMapper.listByBoardIdsOfficial(List.of(boardId), offset, size);
        }
        if ("time_asc".equals(sort)) {
            return postMapper.listByBoardTimeAsc(boardId, offset, size);
        }
        if ("hot".equals(sort)) {
            return postMapper.listByBoardHot(boardId, offset, size);
        }
        return postMapper.listByBoard(boardId, offset, size);
    }

    private List<Post> listBoardIdsWithSort(List<Long> boardIds, int offset, int size, String sort, boolean official) {
        if (official) {
            return postMapper.listByBoardIdsOfficial(boardIds, offset, size);
        }
        if ("time_asc".equals(sort)) {
            return postMapper.listByBoardIdsTimeAsc(boardIds, offset, size);
        }
        if ("hot".equals(sort)) {
            return postMapper.listByBoardIdsHot(boardIds, offset, size);
        }
        return postMapper.listByBoardIds(boardIds, offset, size);
    }

    private void applyProfilePinFlags(List<PostVO> list, Long authorId) {
        if (authorId == null || list == null || list.isEmpty()) {
            return;
        }
        Set<Long> pinned = new HashSet<>(userProfilePinMapper.listPostIdsByUser(authorId));
        for (PostVO vo : list) {
            vo.setProfilePinned(pinned.contains(vo.getId()));
        }
    }

    private void refreshFeaturedStatus(Post post) {
        if (post == null) {
            return;
        }
        if (isOfficialBoard(post.getBoardId())) {
            if (post.getFeatured() != null && post.getFeatured() == 1) {
                postMapper.updateFeatured(post.getId(), 0, null);
                invalidatePostCache(post.getId());
            }
            return;
        }
        double score = computeFeaturedScore(post);
        boolean shouldFeature = score >= ForumConstants.FEATURED_HOT_THRESHOLD;
        boolean currentlyFeatured = post.getFeatured() != null && post.getFeatured() == 1;
        if (shouldFeature && !currentlyFeatured) {
            postMapper.updateFeatured(post.getId(), 1, LocalDateTime.now());
            invalidatePostCache(post.getId());
        } else if (!shouldFeature && currentlyFeatured) {
            postMapper.updateFeatured(post.getId(), 0, null);
            invalidatePostCache(post.getId());
        }
    }

    private double computeFeaturedScore(Post post) {
        return countOrZero(post.getViewCount()) * 0.1
                + countOrZero(post.getLikeCount()) * 2.0
                + favoriteMapper.countByPost(post.getId()) * 2.0
                + countOrZero(post.getReplyCount()) * 1.5;
    }

    private boolean isAllBoard(Long boardId) {
        Board board = boardMapper.findById(boardId);
        return board != null && ForumConstants.BOARD_TYPE_ALL.equals(board.getBoardType());
    }

    private boolean isCategoryBoard(Long boardId) {
        Board board = boardMapper.findById(boardId);
        return board != null && ForumConstants.BOARD_TYPE_CATEGORY.equals(board.getBoardType());
    }

    private BoardVO toBoardVO(Board board) {
        BoardVO vo = new BoardVO();
        vo.setId(board.getId());
        vo.setName(board.getName());
        vo.setDescription(board.getDescription());
        vo.setBoardType(board.getBoardType());
        vo.setSortOrder(board.getSortOrder());
        vo.setParentId(board.getParentId());
        return vo;
    }

    private void invalidatePostCache(Long postId) {
        redisTemplate.delete(ForumConstants.REDIS_POST_DETAIL + postId);
    }

    private void sendLikeMessage(Long fromUserId, Long toUserId, Long refId, String refType, Long postId, String content) {
        String fromName = resolveAuthorName(fromUserId);
        ForumMessage msg = new ForumMessage();
        msg.setType(ForumConstants.TAG_LIKE);
        msg.setFromUserId(fromUserId);
        msg.setFromNickname(fromName);
        msg.setToUserId(toUserId);
        msg.setRefId(refId);
        msg.setRefType(refType);
        msg.setPostId(postId);
        msg.setContent(fromName + " " + content);
        messageProducer.send(ForumConstants.TAG_LIKE, msg);
        deliverNotification(msg);
    }

    private void deliverNotification(ForumMessage msg) {
        if (msg.getToUserId() == null) {
            return;
        }
        if (msg.getFromUserId() != null && msg.getFromUserId().equals(msg.getToUserId())) {
            return;
        }
        try {
            NotificationCreateRequest request = new NotificationCreateRequest();
            request.setUserId(msg.getToUserId());
            request.setType(msg.getType());
            request.setRefId(msg.getRefId());
            request.setRefType(msg.getRefType());
            request.setPostId(resolveNotificationPostId(msg));
            request.setFromUserId(msg.getFromUserId());
            request.setContent(msg.getContent());
            notificationClient.create(request);
        } catch (Exception ignored) {
        }
    }

    private void deliverSystemNotification(Long toUserId, String type, Long refId, String refType, Long postId, String content) {
        if (toUserId == null) {
            return;
        }
        try {
            NotificationCreateRequest request = new NotificationCreateRequest();
            request.setUserId(toUserId);
            request.setType(type);
            request.setRefId(refId);
            request.setRefType(refType);
            request.setPostId(postId != null ? postId : ("POST".equals(refType) ? refId : null));
            request.setContent(content);
            notificationClient.create(request);
        } catch (Exception ignored) {
        }
    }

    private Long resolveNotificationPostId(ForumMessage msg) {
        if (msg.getPostId() != null) {
            return msg.getPostId();
        }
        if ("POST".equals(msg.getRefType())) {
            return msg.getRefId();
        }
        return null;
    }

    private void processMentions(Long fromUserId, String fromNickname, Long postId, Long replyId, String content, String refType) {
        Set<Long> userIds = parseMentionUserIds(content);
        if (userIds.isEmpty()) {
            return;
        }
        saveMentions(postId, replyId, userIds);
        sendMentionNotifications(fromUserId, fromNickname, postId, replyId, refType, userIds);
    }

    private void syncPostMentions(Long fromUserId, String fromNickname, Long postId, String content) {
        Set<Long> newIds = parseMentionUserIds(content);
        Set<Long> oldIds = new HashSet<>(mentionMapper.listUserIdsByPostId(postId));
        mentionMapper.deleteByPostId(postId);
        saveMentions(postId, null, newIds);
        Set<Long> toNotify = new LinkedHashSet<>(newIds);
        toNotify.removeAll(oldIds);
        sendMentionNotifications(fromUserId, fromNickname, postId, null, "POST", toNotify);
    }

    private void saveMentions(Long postId, Long replyId, Set<Long> userIds) {
        for (Long mentionedUserId : userIds) {
            mentionMapper.insert(postId, replyId, mentionedUserId);
        }
    }

    private void sendMentionNotifications(Long fromUserId, String fromNickname, Long postId, Long replyId,
                                          String refType, Set<Long> userIds) {
        for (Long mentionedUserId : userIds) {
            ForumMessage msg = new ForumMessage();
            msg.setType(ForumConstants.TAG_MENTION);
            msg.setFromUserId(fromUserId);
            msg.setFromNickname(fromNickname);
            msg.setToUserId(mentionedUserId);
            msg.setRefId(replyId != null ? replyId : postId);
            msg.setRefType(refType);
            msg.setPostId(postId);
            msg.setContent(fromNickname + " 在内容中 @ 了你");
            messageProducer.send(ForumConstants.TAG_MENTION, msg);
            deliverNotification(msg);
        }
    }

    private Set<Long> parseMentionUserIds(String content) {
        Set<Long> userIds = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return userIds;
        }
        Matcher idMatcher = MENTION_ID_PATTERN.matcher(content);
        while (idMatcher.find()) {
            userIds.add(Long.parseLong(idMatcher.group(1)));
        }
        String legacyContent = MENTION_ID_PATTERN.matcher(content).replaceAll(" ");
        Matcher matcher = MENTION_PATTERN.matcher(legacyContent);
        Set<String> nicknames = new HashSet<>();
        while (matcher.find()) {
            nicknames.add(matcher.group(1));
        }
        if (!nicknames.isEmpty()) {
            Result<List<UserBrief>> result = userClient.batchUsers(new ArrayList<>(nicknames));
            if (result != null && result.getData() != null) {
                for (UserBrief user : result.getData()) {
                    userIds.add(user.getId());
                }
            }
        }
        return userIds;
    }

    private void updateHotScore(Post post) {
        double score = countOrZero(post.getViewCount()) * 0.1
                + countOrZero(post.getLikeCount()) * 2.0
                + countOrZero(post.getReplyCount()) * 1.5;
        String allKey = ForumConstants.REDIS_HOT_POSTS + "all";
        String boardKey = ForumConstants.REDIS_HOT_POSTS + post.getBoardId();
        redisTemplate.opsForZSet().add(allKey, post.getId(), score);
        redisTemplate.opsForZSet().add(boardKey, post.getId(), score);
        refreshFeaturedStatus(post);
    }

    private static int countOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private Post readCachedPost(String cacheKey) {
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached == null) {
            return null;
        }
        if (cached instanceof Post post) {
            return post;
        }
        return postRedisObjectMapper.get().convertValue(cached, Post.class);
    }

    private PostVO enrichPost(Post post, Long currentUserId) {
        PostVO vo = PostVO.from(post);
        vo.setBoardName(resolveBoardName(post.getBoardId()));
        applyAuthorDisplay(vo, post);
        if (currentUserId != null) {
            vo.setLiked(likeMapper.existsPostLike(post.getId(), currentUserId) > 0);
            vo.setFavorited(favoriteMapper.exists(post.getId(), currentUserId) > 0);
        }
        vo.setTags(tagService.resolveTagsForPost(post.getId(), post.getContent()));
        vo.setMentionedUserIds(mentionMapper.listUserIdsByPostId(post.getId()));
        return vo;
    }

    private ReplyVO enrichReply(Reply reply, Long currentUserId) {
        return enrichReply(reply, currentUserId, null);
    }

    private ReplyVO enrichReply(Reply reply, Long currentUserId, Long pinnedReplyId) {
        ReplyVO vo = ReplyVO.from(reply);
        vo.setAuthorName(resolveAuthorName(reply.getAuthorId()));
        vo.setPinned(pinnedReplyId != null && pinnedReplyId.equals(reply.getId()));
        if (currentUserId != null) {
            vo.setLiked(likeMapper.existsReplyLike(reply.getId(), currentUserId) > 0);
            vo.setDisliked(likeMapper.existsReplyDislike(reply.getId(), currentUserId) > 0);
        }
        if (reply.getChildren() != null && !reply.getChildren().isEmpty()) {
            vo.setChildren(reply.getChildren().stream().map(c -> enrichReply(c, currentUserId, pinnedReplyId)).collect(Collectors.toList()));
        }
        vo.setMentionedUserIds(mentionMapper.listUserIdsByReplyId(reply.getId()));
        return vo;
    }

    private String resolveAuthorName(Long userId) {
        try {
            Result<UserBrief> result = userClient.getUser(userId);
            if (result != null && result.getData() != null) {
                return result.getData().getNickname();
            }
        } catch (Exception ignored) {
        }
        return "用户" + userId;
    }

    private String resolveBoardName(Long boardId) {
        Board board = boardMapper.findById(boardId);
        if (board == null) {
            return "未知分区";
        }
        if (board.getParentId() != null) {
            Board parent = boardMapper.findById(board.getParentId());
            if (parent != null) {
                return parent.getName() + "/" + board.getName();
            }
        }
        return board.getName();
    }

    private boolean allowsEmptyContent(Long boardId) {
        Board board = boardMapper.findById(boardId);
        if (board == null) {
            return false;
        }
        return ForumConstants.BOARD_INTERACTIVE_QA.equals(board.getName())
                || "本地问答".equals(board.getName());
    }

    private String normalizePostContent(Long boardId, String content) {
        if (content == null) {
            return allowsEmptyContent(boardId) ? "" : null;
        }
        return PostContentNormalizer.trimPostContent(content);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private void assertNotSimilarRecentPost(Long userId, String role, String title, String content,
                                            Long excludePostId, Boolean confirmSimilar) {
        if (Boolean.TRUE.equals(confirmSimilar) || ForumConstants.ROLE_ADMIN.equals(role)) {
            return;
        }
        if (isSimilarToRecentPost(userId, role, title, content, excludePostId)) {
            throw new BusinessException("您刚刚发布过相似内容，请确认是否继续发布");
        }
    }

    private boolean isSimilarToRecentPost(Long userId, String role, String title, String content, Long excludePostId) {
        if (ForumConstants.ROLE_ADMIN.equals(role) || userId == null) {
            return false;
        }
        LocalDateTime since = LocalDateTime.now().minusMinutes(ForumConstants.POST_SIMILAR_LOOKBACK_MINUTES);
        List<Post> recent = postMapper.listRecentByAuthor(userId, since, 10);
        if (recent.isEmpty()) {
            return false;
        }
        String normTitle = TextSimilarity.normalize(title);
        String normContent = TextSimilarity.normalize(content);
        for (Post post : recent) {
            if (excludePostId != null && excludePostId.equals(post.getId())) {
                continue;
            }
            if (TextSimilarity.similarity(normTitle, TextSimilarity.normalize(post.getTitle()))
                    >= ForumConstants.POST_SIMILAR_THRESHOLD) {
                return true;
            }
            if (TextSimilarity.similarity(normContent, TextSimilarity.normalize(post.getContent()))
                    >= ForumConstants.POST_SIMILAR_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private void validatePostContent(Long boardId, String content) {
        if (allowsEmptyContent(boardId)) {
            return;
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException("内容不能为空");
        }
    }

    private void sortReplyTree(List<ReplyVO> replies, String sort, Long currentUserId, boolean prioritizeOwnTopLevel, Long pinnedReplyId) {
        if (replies == null || replies.isEmpty()) {
            return;
        }
        ReplyVO pinned = null;
        if (pinnedReplyId != null) {
            for (int i = 0; i < replies.size(); i++) {
                if (pinnedReplyId.equals(replies.get(i).getId())) {
                    pinned = replies.remove(i);
                    break;
                }
            }
        }
        if (prioritizeOwnTopLevel && currentUserId != null) {
            List<ReplyVO> mine = replies.stream()
                    .filter(r -> currentUserId.equals(r.getAuthorId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            List<ReplyVO> others = replies.stream()
                    .filter(r -> !currentUserId.equals(r.getAuthorId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            sortReplyList(mine, sort);
            sortReplyList(others, sort);
            replies.clear();
            replies.addAll(mine);
            replies.addAll(others);
        } else {
            sortReplyList(replies, sort);
        }
        if (pinned != null) {
            replies.add(0, pinned);
        }
        for (ReplyVO reply : replies) {
            if (reply.getChildren() != null && !reply.getChildren().isEmpty()) {
                sortReplyTree(reply.getChildren(), sort, currentUserId, false, null);
            }
        }
    }

    private void sortReplyList(List<ReplyVO> replies, String sort) {
        if ("hot".equalsIgnoreCase(sort)) {
            replies.sort((a, b) -> Integer.compare(replyHotScore(b), replyHotScore(a)));
            return;
        }
        replies.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                return 0;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
    }

    private int replyHotScore(ReplyVO reply) {
        int likes = reply.getLikeCount() == null ? 0 : reply.getLikeCount();
        int dislikes = reply.getDislikeCount() == null ? 0 : reply.getDislikeCount();
        return likes - dislikes;
    }

    private void maybeAutoModerateReply(Reply reply, boolean removedLike) {
        int likes = (reply.getLikeCount() == null ? 0 : reply.getLikeCount()) - (removedLike ? 1 : 0);
        int dislikes = (reply.getDislikeCount() == null ? 0 : reply.getDislikeCount()) + 1;
        if (dislikes < ForumConstants.REPLY_MODERATION_MIN_DISLIKES) {
            return;
        }
        if (dislikes <= likes * ForumConstants.REPLY_MODERATION_DISLIKE_RATIO) {
            return;
        }
        systemRemoveReply(reply);
    }

    private void systemRemoveReply(Reply reply) {
        replyMapper.softDelete(reply.getId());
        postMapper.updateReplyCount(reply.getPostId(), -1);
        Post post = postMapper.findById(reply.getPostId());
        if (post != null && reply.getId().equals(post.getPinnedReplyId())) {
            postMapper.updatePinnedReply(reply.getPostId(), null);
        }
        if (post != null) {
            refreshFeaturedStatus(postMapper.findById(reply.getPostId()));
        }
        invalidatePostCache(reply.getPostId());
        String snippet = truncateNotificationText(reply.getContent());
        deliverSystemNotification(
                reply.getAuthorId(),
                ForumConstants.TAG_REPLY_MODERATED,
                reply.getPostId(),
                "POST",
                reply.getPostId(),
                "您的回复：" + snippet + " 经过评审暂不符合社区规范，目前已被删除。请遵守论坛规则、规范发言！"
        );
    }

    private String truncateNotificationText(String text) {
        if (text == null) {
            return "";
        }
        String plain = text
                .replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "[图片]")
                .replaceAll("<video[^>]*>.*?</video>", "[视频]")
                .replaceAll("\\s+", " ")
                .trim();
        if (plain.length() <= 200) {
            return plain;
        }
        return plain.substring(0, 200) + "...";
    }
}
