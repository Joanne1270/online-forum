package com.forum.notification.controller;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.NotificationCreateRequest;
import com.forum.common.dto.PageResult;
import com.forum.common.dto.Result;
import com.forum.notification.entity.Notification;
import com.forum.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<PageResult<Notification>> list(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) String category) {
        return Result.ok(notificationService.list(userId, page, size, category));
    }

    @GetMapping("/unread-count")
    public Result<Long> unread(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        return Result.ok(notificationService.unreadCount(userId));
    }

    @GetMapping("/unread-summary")
    public Result<com.forum.notification.dto.NotificationUnreadSummaryVO> unreadSummary(
            @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        return Result.ok(notificationService.unreadSummary(userId));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id,
                                 @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        notificationService.markRead(id, userId);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                    @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            notificationService.markAllReadByCategory(userId, category);
        } else {
            notificationService.markAllRead(userId);
        }
        return Result.ok();
    }

    @PostMapping("/internal")
    public Result<Void> createInternal(@RequestBody NotificationCreateRequest request) {
        notificationService.create(request);
        return Result.ok();
    }
}
