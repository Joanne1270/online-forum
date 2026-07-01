package com.forum.post.dto;

import lombok.Data;

@Data
public class BannedWordVO {

    private Long id;
    private String word;
    private Integer enabled;
}
