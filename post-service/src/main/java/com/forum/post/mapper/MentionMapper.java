package com.forum.post.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MentionMapper {

    @Insert("INSERT INTO mention(post_id, reply_id, mentioned_user_id) VALUES(#{postId}, #{replyId}, #{mentionedUserId})")
    int insert(@Param("postId") Long postId, @Param("replyId") Long replyId, @Param("mentionedUserId") Long mentionedUserId);

    @Select("SELECT mentioned_user_id FROM mention WHERE reply_id = #{replyId}")
    List<Long> listUserIdsByReplyId(@Param("replyId") Long replyId);

    @Select("SELECT mentioned_user_id FROM mention WHERE post_id = #{postId} AND reply_id IS NULL")
    List<Long> listUserIdsByPostId(@Param("postId") Long postId);

    @Delete("DELETE FROM mention WHERE post_id = #{postId} AND reply_id IS NULL")
    int deleteByPostId(@Param("postId") Long postId);
}
