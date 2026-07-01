package com.forum.post.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserProfilePinMapper {

    @Insert("INSERT INTO user_profile_pin(user_id, post_id, sort_order) VALUES(#{userId}, #{postId}, #{sortOrder})")
    int insert(@Param("userId") Long userId, @Param("postId") Long postId, @Param("sortOrder") int sortOrder);

    @Delete("DELETE FROM user_profile_pin WHERE user_id = #{userId} AND post_id = #{postId}")
    int delete(@Param("userId") Long userId, @Param("postId") Long postId);

    @Delete("DELETE FROM user_profile_pin WHERE post_id = #{postId}")
    int deleteByPostId(Long postId);

    @Select("SELECT COUNT(*) FROM user_profile_pin WHERE user_id = #{userId}")
    int countByUser(Long userId);

    @Select("SELECT COUNT(*) FROM user_profile_pin WHERE user_id = #{userId} AND post_id = #{postId}")
    int exists(@Param("userId") Long userId, @Param("postId") Long postId);

    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM user_profile_pin WHERE user_id = #{userId}")
    int maxSortOrder(Long userId);

    @Select("SELECT post_id FROM user_profile_pin WHERE user_id = #{userId} ORDER BY sort_order ASC, id ASC")
    List<Long> listPostIdsByUser(Long userId);
}
