package com.forum.notification.service;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.NotificationCreateRequest;
import com.forum.common.dto.PageResult;
import com.forum.notification.dto.NotificationUnreadSummaryVO;
import com.forum.notification.entity.Notification;
import com.forum.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public void save(Notification notification) {
        notificationMapper.insert(notification);
    }

    public void create(NotificationCreateRequest request) {
        if (request.getUserId() == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setRefId(request.getRefId());
        notification.setRefType(request.getRefType());
        notification.setPostId(request.getPostId());
        notification.setFromUserId(request.getFromUserId());
        notification.setContent(request.getContent());
        notificationMapper.insert(notification);
    }

    public PageResult<Notification> list(Long userId, int page, int size, String category) {
        int offset = (page - 1) * size;
        List<String> types = resolveTypes(category);
        if (types == null) {
            long total = notificationMapper.countByUser(userId);
            return new PageResult<>(notificationMapper.listByUser(userId, offset, size), total, page, size);
        }
        long total = notificationMapper.countByUserAndTypes(userId, types);
        return new PageResult<>(notificationMapper.listByUserAndTypes(userId, types, offset, size), total, page, size);
    }

    public long unreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    public NotificationUnreadSummaryVO unreadSummary(Long userId) {
        NotificationUnreadSummaryVO summary = new NotificationUnreadSummaryVO();
        summary.setReply(notificationMapper.countUnreadByTypes(userId, typesFor("reply")));
        summary.setMention(notificationMapper.countUnreadByTypes(userId, typesFor("mention")));
        summary.setLike(notificationMapper.countUnreadByTypes(userId, typesFor("like")));
        summary.setSystem(notificationMapper.countUnreadByTypes(userId, typesFor("system")));
        Map<String, Long> byCategory = new LinkedHashMap<>();
        byCategory.put("reply", summary.getReply());
        byCategory.put("mention", summary.getMention());
        byCategory.put("like", summary.getLike());
        byCategory.put("system", summary.getSystem());
        summary.setByCategory(byCategory);
        return summary;
    }

    public void markRead(Long id, Long userId) {
        notificationMapper.markRead(id, userId);
    }

    public void markAllRead(Long userId) {
        notificationMapper.markAllRead(userId);
    }

    public void markAllReadByCategory(Long userId, String category) {
        List<String> types = resolveTypes(category);
        if (types == null || types.isEmpty()) {
            notificationMapper.markAllRead(userId);
            return;
        }
        notificationMapper.markAllReadByTypes(userId, types);
    }

    private List<String> resolveTypes(String category) {
        if (category == null || category.isBlank() || "all".equals(category)) {
            return null;
        }
        return typesFor(category);
    }

    private List<String> typesFor(String category) {
        return switch (category) {
            case "reply" -> List.of(ForumConstants.TAG_REPLY_CREATED);
            case "mention" -> List.of(ForumConstants.TAG_MENTION);
            case "like" -> List.of(ForumConstants.TAG_LIKE, ForumConstants.TAG_FAVORITE);
            case "system" -> List.of(
                    ForumConstants.TAG_POST_DELETED,
                    ForumConstants.TAG_REPLY_MODERATED,
                    ForumConstants.TAG_REPORT_RESULT,
                    ForumConstants.TAG_PROFILE_APPROVED,
                    ForumConstants.TAG_PROFILE_REJECTED
            );
            default -> List.of();
        };
    }
}
