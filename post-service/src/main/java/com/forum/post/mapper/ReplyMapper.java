package com.forum.post.mapper;

import com.forum.post.entity.Reply;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReplyMapper {

    @Select("SELECT * FROM reply WHERE post_id = #{postId} AND status = 1 ORDER BY created_at ASC")
    List<Reply> listByPostId(Long postId);

    @Select("SELECT r.* FROM reply r INNER JOIN post p ON r.post_id = p.id AND p.status = 1 "
            + "WHERE r.author_id = #{authorId} AND r.status = 1 ORDER BY r.created_at DESC LIMIT #{offset}, #{size}")
    List<Reply> listByAuthor(@Param("authorId") Long authorId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT * FROM reply WHERE id = #{id} AND status = 1")
    Reply findById(Long id);

    @Select("SELECT post_id FROM reply WHERE id = #{id}")
    Long findPostIdById(Long id);

    @Insert("INSERT INTO reply(post_id, parent_id, author_id, content, like_count, status) "
            + "VALUES(#{postId}, #{parentId}, #{authorId}, #{content}, 0, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Reply reply);

    @Update("UPDATE reply SET content=#{content} WHERE id=#{id} AND author_id=#{authorId} AND status=1")
    int update(Reply reply);

    @Update("UPDATE reply SET status=0 WHERE id=#{id}")
    int softDelete(Long id);

    @Update("UPDATE reply SET status=0 WHERE post_id=#{postId} AND status=1")
    int softDeleteByPostId(Long postId);

    @Update("UPDATE reply SET like_count = GREATEST(0, like_count + #{delta}) WHERE id = #{id}")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE reply SET dislike_count = GREATEST(0, dislike_count + #{delta}) WHERE id = #{id}")
    int updateDislikeCount(@Param("id") Long id, @Param("delta") int delta);
}
