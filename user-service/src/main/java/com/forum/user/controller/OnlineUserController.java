package com.forum.user.controller;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.Result;
import com.forum.user.dto.OnlineStatsVO;
import com.forum.user.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    @PostMapping("/api/users/me/heartbeat")
    public Result<Void> heartbeat(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) {
        onlineUserService.heartbeat(userId);
        return Result.ok();
    }

    @GetMapping("/api/admin/online-stats")
    public Result<OnlineStatsVO> onlineStats(@RequestParam(defaultValue = "day") String range) {
        return Result.ok(onlineUserService.getStats(range));
    }
}
