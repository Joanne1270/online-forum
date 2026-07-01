package com.forum.file.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileMeta {

    private Long id;
    private String originalName;
    private String storedName;
    private String url;
    private Long uploaderId;
    private Long size;
    private String mime;
    private LocalDateTime createdAt;
}
