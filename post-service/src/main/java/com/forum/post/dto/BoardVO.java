package com.forum.post.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BoardVO {

    private Long id;
    private String name;
    private String description;
    private String boardType;
    private Integer sortOrder;
    private Long parentId;
    private List<BoardVO> children = new ArrayList<>();
}
