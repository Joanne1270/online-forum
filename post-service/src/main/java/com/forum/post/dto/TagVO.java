package com.forum.post.dto;

import com.forum.post.entity.Tag;
import lombok.Data;

@Data
public class TagVO {

    private Long id;
    private String name;
    private Integer usageCount;

    public static TagVO from(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setUsageCount(tag.getUsageCount());
        return vo;
    }
}
