package com.forum.user.client;

import com.forum.common.dto.PageResult;
import com.forum.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "post-service")
public interface PostClient {

    @GetMapping("/api/posts/by-author/{authorId}")
    Result<PageResult<Map<String, Object>>> postsByAuthor(@PathVariable("authorId") Long authorId,
                                                          @RequestParam("page") int page,
                                                          @RequestParam("size") int size,
                                                          @RequestHeader(value = "X-User-Id", required = false) Long viewerId);

    @GetMapping("/api/posts/favorites/by-user/{userId}")
    Result<PageResult<Map<String, Object>>> favoritesByUser(@PathVariable("userId") Long userId,
                                                            @RequestParam("page") int page,
                                                            @RequestParam("size") int size,
                                                            @RequestHeader(value = "X-User-Id", required = false) Long viewerId);

    @GetMapping("/api/replies/by-author/{authorId}")
    Result<List<Map<String, Object>>> repliesByAuthor(@PathVariable("authorId") Long authorId,
                                                       @RequestParam("page") int page,
                                                       @RequestParam("size") int size,
                                                       @RequestHeader(value = "X-User-Id", required = false) Long viewerId);
}
