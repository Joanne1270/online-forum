package com.forum.admin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminBoardVO {

    private Long id;
    private String name;
    private String description;
    private String boardType;
    private Long parentId;
    private Integer sortOrder;
    private List<AdminBoardVO> children = new ArrayList<>();
}
