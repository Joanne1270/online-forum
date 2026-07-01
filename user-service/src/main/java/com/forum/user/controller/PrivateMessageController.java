package com.forum.user.controller;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.PageResult;
import com.forum.common.dto.Result;
import com.forum.user.dto.ConversationVO;
import com.forum.user.dto.PrivateMessageVO;
import com.forum.user.dto.SendMessageRequest;
import com.forum.user.service.PrivateMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/messages")
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    @GetMapping("/conversations")
    public Result<List<ConversationVO>> conversations(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                                      @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        return Result.ok(privateMessageService.listConversations(userId, role));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                   @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role) {
        return Result.ok(privateMessageService.unreadCount(userId, role));
    }

    @GetMapping("/with/{peerId}")
    public Result<PageResult<PrivateMessageVO>> listWithPeer(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                                             @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                                             @PathVariable Long peerId,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "50") int size) {
        return Result.ok(privateMessageService.listWithPeer(userId, role, peerId, page, size));
    }

    @PostMapping("/with/{peerId}")
    public Result<PrivateMessageVO> send(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                         @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                         @PathVariable Long peerId,
                                         @RequestBody SendMessageRequest request) {
        return Result.ok(privateMessageService.send(userId, role, peerId, request.getContent()));
    }

    @PutMapping("/with/{peerId}/read")
    public Result<Void> markRead(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                 @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role,
                                 @PathVariable Long peerId) {
        privateMessageService.markRead(userId, role, peerId);
        return Result.ok();
    }
}
