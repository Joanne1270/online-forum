package com.forum.post.mapper;

import com.forum.post.dto.UserReportVO;
import com.forum.post.entity.ReplyReport;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReplyReportMapper {

    @Insert("INSERT INTO reply_report(reply_id, reporter_id, reason, status) VALUES(#{replyId}, #{reporterId}, #{reason}, 0)")
    int insert(ReplyReport report);

    @Select("SELECT COUNT(*) FROM reply_report WHERE reply_id = #{replyId} AND reporter_id = #{reporterId}")
    int exists(@Param("replyId") Long replyId, @Param("reporterId") Long reporterId);

    @Select("SELECT rr.id, 'REPLY' AS refType, rep.post_id AS postId, p.title AS title, rr.reason, rr.status, rr.created_at AS createdAt "
            + "FROM reply_report rr INNER JOIN reply rep ON rep.id = rr.reply_id "
            + "LEFT JOIN post p ON p.id = rep.post_id "
            + "WHERE rr.reporter_id = #{reporterId} ORDER BY rr.created_at DESC LIMIT #{offset}, #{size}")
    List<UserReportVO> listByReporter(@Param("reporterId") Long reporterId,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    @Select("SELECT COUNT(*) FROM reply_report WHERE reporter_id = #{reporterId}")
    long countByReporter(Long reporterId);
}
