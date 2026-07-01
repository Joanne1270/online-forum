package com.forum.post.mapper;

import com.forum.post.entity.Post;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PostMapper {

    @Select("SELECT * FROM post WHERE id = #{id} AND status = 1")
    Post findById(Long id);

    @Select("SELECT * FROM post WHERE board_id = #{boardId} AND status = 1 ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<Post> listByBoard(@Param("boardId") Long boardId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT * FROM post WHERE board_id = #{boardId} AND status = 1 "
            + "ORDER BY (COALESCE(view_count,0)*0.1 + COALESCE(like_count,0)*2.0 + "
            + "(SELECT COUNT(*) FROM post_favorite pf WHERE pf.post_id = post.id)*2.0 + COALESCE(reply_count,0)*1.5) DESC, id DESC "
            + "LIMIT #{offset}, #{size}")
    List<Post> listByBoardHot(@Param("boardId") Long boardId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT * FROM post WHERE board_id = #{boardId} AND status = 1 ORDER BY created_at ASC LIMIT #{offset}, #{size}")
    List<Post> listByBoardTimeAsc(@Param("boardId") Long boardId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM post WHERE board_id = #{boardId} AND status = 1")
    long countByBoard(Long boardId);

    @Select("<script>SELECT * FROM post WHERE status = 1 AND board_id IN "
            + "<foreach collection='boardIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY created_at DESC LIMIT #{offset}, #{size}</script>")
    List<Post> listByBoardIds(@Param("boardIds") List<Long> boardIds, @Param("offset") int offset, @Param("size") int size);

    @Select("<script>SELECT p.* FROM post p WHERE p.status = 1 AND p.board_id IN "
            + "<foreach collection='boardIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY p.official_pinned DESC, p.official_pinned_at DESC, p.created_at DESC "
            + "LIMIT #{offset}, #{size}</script>")
    List<Post> listByBoardIdsOfficial(@Param("boardIds") List<Long> boardIds, @Param("offset") int offset, @Param("size") int size);

    @Select("<script>SELECT p.* FROM post p WHERE p.status = 1 AND p.board_id IN "
            + "<foreach collection='boardIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY (COALESCE(p.view_count,0)*0.1 + COALESCE(p.like_count,0)*2.0 + "
            + "(SELECT COUNT(*) FROM post_favorite pf WHERE pf.post_id = p.id)*2.0 + COALESCE(p.reply_count,0)*1.5) DESC, p.id DESC "
            + "LIMIT #{offset}, #{size}</script>")
    List<Post> listByBoardIdsHot(@Param("boardIds") List<Long> boardIds, @Param("offset") int offset, @Param("size") int size);

    @Select("<script>SELECT p.* FROM post p WHERE p.status = 1 AND p.board_id IN "
            + "<foreach collection='boardIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY p.created_at ASC LIMIT #{offset}, #{size}</script>")
    List<Post> listByBoardIdsTimeAsc(@Param("boardIds") List<Long> boardIds, @Param("offset") int offset, @Param("size") int size);

    @Select("<script>SELECT COUNT(*) FROM post WHERE status = 1 AND board_id IN "
            + "<foreach collection='boardIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    long countByBoardIds(@Param("boardIds") List<Long> boardIds);

    @Select("SELECT p.* FROM post p LEFT JOIN user_profile_pin upp ON upp.post_id = p.id AND upp.user_id = #{authorId} "
            + "WHERE p.author_id = #{authorId} AND p.status = 1 "
            + "ORDER BY CASE WHEN upp.id IS NOT NULL THEN 0 ELSE 1 END, upp.sort_order ASC, p.created_at DESC "
            + "LIMIT #{offset}, #{size}")
    List<Post> listByAuthor(@Param("authorId") Long authorId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT p.* FROM post p INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN user_profile_pin upp ON upp.post_id = p.id AND upp.user_id = #{authorId} "
            + "WHERE p.author_id = #{authorId} AND p.status = 1 AND b.board_type <> 'HOME' "
            + "ORDER BY CASE WHEN upp.id IS NOT NULL THEN 0 ELSE 1 END, upp.sort_order ASC, p.created_at DESC "
            + "LIMIT #{offset}, #{size}")
    List<Post> listPublicByAuthor(@Param("authorId") Long authorId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM post p INNER JOIN board b ON p.board_id = b.id "
            + "WHERE p.author_id = #{authorId} AND p.status = 1 AND b.board_type <> 'HOME'")
    long countPublicByAuthor(@Param("authorId") Long authorId);

    @Select("SELECT COUNT(*) FROM post WHERE author_id = #{authorId} AND status = 1")
    long countByAuthor(@Param("authorId") Long authorId);

    @Select("SELECT * FROM post WHERE id = #{id} AND status = 2 AND author_id = #{authorId}")
    Post findDraftById(@Param("id") Long id, @Param("authorId") Long authorId);

    @Select("SELECT * FROM post WHERE author_id = #{authorId} AND status = 2 "
            + "ORDER BY COALESCE(updated_at, created_at) DESC LIMIT #{offset}, #{size}")
    List<Post> listDraftsByAuthor(@Param("authorId") Long authorId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM post WHERE author_id = #{authorId} AND status = 2")
    long countDraftsByAuthor(@Param("authorId") Long authorId);

    @Insert("INSERT INTO post(board_id, author_id, title, content, view_count, like_count, reply_count, status) "
            + "VALUES(#{boardId}, #{authorId}, #{title}, #{content}, 0, 0, 0, 2)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDraft(Post post);

    @Update("UPDATE post SET board_id=#{boardId}, title=#{title}, content=#{content}, updated_at=NOW() "
            + "WHERE id=#{id} AND author_id=#{authorId} AND status=2")
    int updateDraft(Post post);

    @Update("UPDATE post SET board_id=#{boardId}, title=#{title}, content=#{content}, status=1, updated_at=NOW() "
            + "WHERE id=#{id} AND author_id=#{authorId} AND status=2")
    int publishDraft(Post post);

    @Select("SELECT p.* FROM post p "
            + "INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN board pb ON b.parent_id = pb.id "
            + "WHERE p.status = 1 AND ("
            + "p.title LIKE CONCAT('%', #{keyword}, '%') "
            + "OR p.content LIKE CONCAT('%', #{keyword}, '%') "
            + "OR b.name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR IFNULL(b.description, '') LIKE CONCAT('%', #{keyword}, '%') "
            + "OR IFNULL(pb.name, '') LIKE CONCAT('%', #{keyword}, '%') "
            + "OR IFNULL(pb.description, '') LIKE CONCAT('%', #{keyword}, '%') "
            + "OR EXISTS (SELECT 1 FROM post_tag pt INNER JOIN tag t ON pt.tag_id = t.id "
            + "WHERE pt.post_id = p.id AND t.name LIKE CONCAT('%', #{keyword}, '%'))) "
            + "ORDER BY p.created_at DESC LIMIT #{offset}, #{size}")
    List<Post> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM post p "
            + "INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN board pb ON b.parent_id = pb.id "
            + "WHERE p.status = 1 AND ("
            + "p.title LIKE CONCAT('%', #{keyword}, '%') "
            + "OR p.content LIKE CONCAT('%', #{keyword}, '%') "
            + "OR b.name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR IFNULL(b.description, '') LIKE CONCAT('%', #{keyword}, '%') "
            + "OR IFNULL(pb.name, '') LIKE CONCAT('%', #{keyword}, '%') "
            + "OR IFNULL(pb.description, '') LIKE CONCAT('%', #{keyword}, '%') "
            + "OR EXISTS (SELECT 1 FROM post_tag pt INNER JOIN tag t ON pt.tag_id = t.id "
            + "WHERE pt.post_id = p.id AND t.name LIKE CONCAT('%', #{keyword}, '%')))")
    long countSearch(String keyword);

    @Select("SELECT DISTINCT p.* FROM post p "
            + "INNER JOIN post_tag pt ON pt.post_id = p.id "
            + "INNER JOIN tag t ON t.id = pt.tag_id "
            + "WHERE p.status = 1 AND t.name = #{tagName} "
            + "ORDER BY p.created_at DESC LIMIT #{offset}, #{size}")
    List<Post> searchByTag(@Param("tagName") String tagName, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(DISTINCT p.id) FROM post p "
            + "INNER JOIN post_tag pt ON pt.post_id = p.id "
            + "INNER JOIN tag t ON t.id = pt.tag_id "
            + "WHERE p.status = 1 AND t.name = #{tagName}")
    long countSearchByTag(String tagName);

    @Select("SELECT p.* FROM post p INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN board pb ON b.parent_id = pb.id "
            + "WHERE p.status = 1 AND b.board_type <> 'HOME' "
            + "AND (pb.id IS NULL OR pb.board_type <> 'HOME') "
            + "ORDER BY p.created_at DESC LIMIT #{offset}, #{size}")
    List<Post> listAll(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT p.* FROM post p INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN board pb ON b.parent_id = pb.id "
            + "WHERE p.status = 1 AND p.featured = 1 AND b.board_type <> 'HOME' "
            + "AND (pb.id IS NULL OR pb.board_type <> 'HOME') "
            + "ORDER BY p.featured_at DESC, p.id DESC LIMIT #{offset}, #{size}")
    List<Post> listAllFeatured(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM post p INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN board pb ON b.parent_id = pb.id "
            + "WHERE p.status = 1 AND b.board_type <> 'HOME' "
            + "AND (pb.id IS NULL OR pb.board_type <> 'HOME')")
    long countAll();

    @Select("SELECT COUNT(*) FROM post p INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN board pb ON b.parent_id = pb.id "
            + "WHERE p.status = 1 AND p.featured = 1 AND b.board_type <> 'HOME' "
            + "AND (pb.id IS NULL OR pb.board_type <> 'HOME')")
    long countAllFeatured();

    @Select("SELECT p.* FROM post p INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN board pb ON b.parent_id = pb.id "
            + "WHERE p.status = 1 AND b.board_type <> 'HOME' "
            + "AND (pb.id IS NULL OR pb.board_type <> 'HOME') "
            + "ORDER BY (COALESCE(p.view_count,0)*0.1 + COALESCE(p.like_count,0)*2.0 + "
            + "(SELECT COUNT(*) FROM post_favorite pf WHERE pf.post_id = p.id)*2.0 + COALESCE(p.reply_count,0)*1.5) DESC, p.id DESC "
            + "LIMIT #{offset}, #{size}")
    List<Post> listAllByHot(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT * FROM post WHERE author_id = #{authorId} AND status = 1 "
            + "AND created_at >= #{since} ORDER BY created_at DESC LIMIT #{limit}")
    List<Post> listRecentByAuthor(@Param("authorId") Long authorId,
                                  @Param("since") java.time.LocalDateTime since,
                                  @Param("limit") int limit);

    @Insert("INSERT INTO post(board_id, author_id, title, content, view_count, like_count, reply_count, status) "
            + "VALUES(#{boardId}, #{authorId}, #{title}, #{content}, 0, 0, 0, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Post post);

    @Update("UPDATE post SET title=#{title}, content=#{content}, updated_at=NOW() WHERE id=#{id} AND author_id=#{authorId} AND status=1")
    int update(Post post);

    @Update("UPDATE post SET status=0 WHERE id=#{id}")
    int softDelete(Long id);

    @Update("UPDATE post SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementView(Long id);

    @Update("UPDATE post SET like_count = GREATEST(0, like_count + #{delta}) WHERE id = #{id}")
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE post SET reply_count = reply_count + #{delta} WHERE id = #{id}")
    int updateReplyCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE post SET featured = #{featured}, featured_at = #{featuredAt} WHERE id = #{id}")
    int updateFeatured(@Param("id") Long id, @Param("featured") int featured, @Param("featuredAt") java.time.LocalDateTime featuredAt);

    @Update("UPDATE post SET official_pinned = #{pinned}, official_pinned_at = #{pinnedAt} WHERE id = #{id}")
    int updateOfficialPinned(@Param("id") Long id, @Param("pinned") int pinned, @Param("pinnedAt") java.time.LocalDateTime pinnedAt);

    @Update("UPDATE post SET pinned_reply_id = #{replyId} WHERE id = #{id}")
    int updatePinnedReply(@Param("id") Long id, @Param("replyId") Long replyId);

    @Select("<script>SELECT p.* FROM post p INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN board pb ON b.parent_id = pb.id "
            + "WHERE p.status = 1 AND b.board_type &lt;&gt; 'HOME' "
            + "AND (pb.id IS NULL OR pb.board_type &lt;&gt; 'HOME') AND p.author_id IN "
            + "<foreach collection='authorIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY p.created_at DESC LIMIT #{offset}, #{size}</script>")
    List<Post> listByAuthors(@Param("authorIds") List<Long> authorIds,
                             @Param("offset") int offset,
                             @Param("size") int size);

    @Select("<script>SELECT COUNT(*) FROM post p INNER JOIN board b ON p.board_id = b.id "
            + "LEFT JOIN board pb ON b.parent_id = pb.id "
            + "WHERE p.status = 1 AND b.board_type &lt;&gt; 'HOME' "
            + "AND (pb.id IS NULL OR pb.board_type &lt;&gt; 'HOME') AND p.author_id IN "
            + "<foreach collection='authorIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    long countByAuthors(@Param("authorIds") List<Long> authorIds);
}
