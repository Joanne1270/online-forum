package com.forum.post.mapper;

import com.forum.post.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TagMapper {

    @Select("SELECT * FROM tag WHERE name = #{name}")
    Tag findByName(String name);

    @Insert("INSERT INTO tag(name, usage_count) VALUES(#{name}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    @Update("UPDATE tag SET usage_count = GREATEST(0, usage_count + #{delta}) WHERE id = #{id}")
    int updateUsageCount(@Param("id") Long id, @Param("delta") int delta);

    @Select("SELECT * FROM tag WHERE name LIKE CONCAT(#{keyword}, '%') "
            + "ORDER BY usage_count DESC, name ASC LIMIT #{limit}")
    List<Tag> search(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("SELECT * FROM tag ORDER BY usage_count DESC, name ASC LIMIT #{limit}")
    List<Tag> listPopular(@Param("limit") int limit);
}
