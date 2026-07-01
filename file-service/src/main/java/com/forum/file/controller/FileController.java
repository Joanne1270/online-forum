package com.forum.file.controller;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.Result;
import com.forum.file.entity.FileMeta;
import com.forum.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public Result<FileMeta> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "category", defaultValue = "media") String category,
                                   @RequestHeader(ForumConstants.HEADER_USER_ID) Long userId) throws Exception {
        return Result.ok(fileService.upload(file, userId, category));
    }
}
