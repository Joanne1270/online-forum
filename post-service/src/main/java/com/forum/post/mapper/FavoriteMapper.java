package com.forum.post.mapper;

import com.forum.post.entity.Post;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FavoriteMapper {

    @Insert("INSERT INTO post_favorite(post_id, user_id) VALUES(#{postId}, #{userId})")
    int insert(@Param("postId") Long postId, @Param("userId") Long userId);

    @Delete("DELETE FROM post_favorite WHERE post_id = #{postId} AND user_id = #{userId}")
    int delete(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_favorite WHERE post_id = #{postId} AND user_id = #{userId}")
    int exists(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_favorite WHERE post_id = #{postId}")
    int countByPost(Long postId);

    @Select("SELECT p.* FROM post p INNER JOIN post_favorite f ON p.id = f.post_id "
            + "WHERE f.user_id = #{userId} AND p.status = 1 ORDER BY f.created_at DESC LIMIT #{offset}, #{size}")
    List<Post> listByUser(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM post_favorite f INNER JOIN post p ON p.id = f.post_id "
            + "WHERE f.user_id = #{userId} AND p.status = 1")
    long countByUser(Long userId);
}
