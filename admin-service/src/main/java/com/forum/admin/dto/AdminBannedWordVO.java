package com.forum.admin.dto;

import lombok.Data;

@Data
public class AdminBannedWordVO {

    private Long id;
    private String word;
    private Integer enabled;
}
