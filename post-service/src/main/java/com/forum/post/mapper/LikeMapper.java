package com.forum.post.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LikeMapper {

    @Insert("INSERT INTO post_like(post_id, user_id) VALUES(#{postId}, #{userId})")
    int insertPostLike(@Param("postId") Long postId, @Param("userId") Long userId);

    @Delete("DELETE FROM post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int deletePostLike(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int existsPostLike(@Param("postId") Long postId, @Param("userId") Long userId);

    @Insert("INSERT INTO reply_like(reply_id, user_id) VALUES(#{replyId}, #{userId})")
    int insertReplyLike(@Param("replyId") Long replyId, @Param("userId") Long userId);

    @Delete("DELETE FROM reply_like WHERE reply_id = #{replyId} AND user_id = #{userId}")
    int deleteReplyLike(@Param("replyId") Long replyId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM reply_like WHERE reply_id = #{replyId} AND user_id = #{userId}")
    int existsReplyLike(@Param("replyId") Long replyId, @Param("userId") Long userId);

    @Insert("INSERT INTO reply_dislike(reply_id, user_id) VALUES(#{replyId}, #{userId})")
    int insertReplyDislike(@Param("replyId") Long replyId, @Param("userId") Long userId);

    @Delete("DELETE FROM reply_dislike WHERE reply_id = #{replyId} AND user_id = #{userId}")
    int deleteReplyDislike(@Param("replyId") Long replyId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM reply_dislike WHERE reply_id = #{replyId} AND user_id = #{userId}")
    int existsReplyDislike(@Param("replyId") Long replyId, @Param("userId") Long userId);

    @Select("SELECT p.* FROM post p INNER JOIN post_like l ON p.id = l.post_id "
            + "WHERE l.user_id = #{userId} AND p.status = 1 ORDER BY l.created_at DESC LIMIT #{offset}, #{size}")
    List<com.forum.post.entity.Post> listLikedPosts(@Param("userId") Long userId,
                                                    @Param("offset") int offset,
                                                    @Param("size") int size);

    @Select("SELECT COUNT(*) FROM post_like l INNER JOIN post p ON p.id = l.post_id "
            + "WHERE l.user_id = #{userId} AND p.status = 1")
    long countLikedPosts(Long userId);
}
