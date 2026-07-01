package com.forum.post.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PostTagMapper {

    @Insert("INSERT IGNORE INTO post_tag(post_id, tag_id) VALUES(#{postId}, #{tagId})")
    int insert(@Param("postId") Long postId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM post_tag WHERE post_id = #{postId}")
    int deleteByPostId(Long postId);

    @Select("SELECT tag_id FROM post_tag WHERE post_id = #{postId}")
    List<Long> listTagIdsByPostId(Long postId);

    @Select("SELECT t.name FROM tag t INNER JOIN post_tag pt ON pt.tag_id = t.id "
            + "WHERE pt.post_id = #{postId} ORDER BY t.name ASC")
    List<String> listTagNamesByPostId(Long postId);
}
