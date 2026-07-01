package com.forum.post.mapper;

import com.forum.post.dto.ReportVO;
import com.forum.post.entity.PostReport;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReportMapper {

    @Insert("INSERT INTO post_report(post_id, reporter_id, reason, status) VALUES(#{postId}, #{reporterId}, #{reason}, 0)")
    int insert(PostReport report);

    @Select("SELECT COUNT(*) FROM post_report WHERE post_id = #{postId} AND reporter_id = #{reporterId}")
    int exists(@Param("postId") Long postId, @Param("reporterId") Long reporterId);

    @Select("SELECT r.id, r.post_id AS postId, p.title AS postTitle, r.reporter_id AS reporterId, "
            + "r.reason, r.status, r.created_at AS createdAt "
            + "FROM post_report r INNER JOIN post p ON p.id = r.post_id "
            + "WHERE r.status = 0 ORDER BY r.created_at DESC LIMIT #{offset}, #{size}")
    List<ReportVO> listPending(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM post_report WHERE status = 0")
    long countPending();

    @Select("SELECT * FROM post_report WHERE id = #{id}")
    PostReport findById(Long id);

    @Update("UPDATE post_report SET status = #{status}, handled_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") int status);

    @Select("SELECT r.id, 'POST' AS refType, r.post_id AS postId, p.title AS title, r.reason, r.status, r.created_at AS createdAt "
            + "FROM post_report r INNER JOIN post p ON p.id = r.post_id "
            + "WHERE r.reporter_id = #{reporterId} ORDER BY r.created_at DESC LIMIT #{offset}, #{size}")
    List<com.forum.post.dto.UserReportVO> listByReporter(@Param("reporterId") Long reporterId,
                                                          @Param("offset") int offset,
                                                          @Param("size") int size);

    @Select("SELECT COUNT(*) FROM post_report WHERE reporter_id = #{reporterId}")
    long countByReporter(Long reporterId);
}
