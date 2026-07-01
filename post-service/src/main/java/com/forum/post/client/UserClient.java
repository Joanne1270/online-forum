package com.forum.post.client;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.Result;
import com.forum.post.dto.UserBrief;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    Result<UserBrief> getUser(@PathVariable("id") Long id);

    @PostMapping("/api/users/batch")
    Result<List<UserBrief>> batchUsers(@RequestBody List<String> nicknames);

    @GetMapping("/api/users/me/following-ids")
    Result<List<Long>> followingIds(@RequestHeader(ForumConstants.HEADER_USER_ID) Long userId,
                                   @RequestHeader(ForumConstants.HEADER_USER_ROLE) String role);
}
