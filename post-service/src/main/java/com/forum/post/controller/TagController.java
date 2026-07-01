package com.forum.post.controller;

import com.forum.common.dto.Result;
import com.forum.post.dto.TagVO;
import com.forum.post.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/api/tags/search")
    public Result<List<TagVO>> search(@RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(tagService.searchTags(keyword, limit));
    }
}
