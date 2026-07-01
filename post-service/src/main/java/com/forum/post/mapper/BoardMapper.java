package com.forum.post.mapper;

import com.forum.post.entity.Board;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BoardMapper {

    @Select("SELECT * FROM board ORDER BY sort_order ASC, id ASC")
    List<Board> findAll();

    @Select("SELECT * FROM board WHERE id = #{id}")
    Board findById(Long id);

    @Select("SELECT id FROM board WHERE parent_id = #{parentId} ORDER BY sort_order ASC, id ASC")
    List<Long> findIdsByParentId(Long parentId);

    @Select("SELECT COUNT(*) FROM board WHERE parent_id = #{parentId}")
    long countByParentId(Long parentId);

    @Insert("INSERT INTO board(name, description, board_type, parent_id, sort_order) "
            + "VALUES(#{name}, #{description}, #{boardType}, #{parentId}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Board board);

    @Update("UPDATE board SET name=#{name}, description=#{description}, board_type=#{boardType}, "
            + "parent_id=#{parentId}, sort_order=#{sortOrder} WHERE id=#{id}")
    int update(Board board);

    @Delete("DELETE FROM board WHERE id = #{id}")
    int delete(Long id);
}
