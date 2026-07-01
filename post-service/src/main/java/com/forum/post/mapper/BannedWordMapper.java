package com.forum.post.mapper;

import com.forum.post.entity.BannedWord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BannedWordMapper {

    @Select("SELECT * FROM banned_word WHERE enabled = 1 ORDER BY id ASC")
    List<BannedWord> listEnabled();

    @Select("SELECT * FROM banned_word ORDER BY id DESC")
    List<BannedWord> listAll();

    @Insert("INSERT INTO banned_word(word, enabled) VALUES(#{word}, 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BannedWord word);

    @Delete("DELETE FROM banned_word WHERE id = #{id}")
    int delete(Long id);

    @Select("SELECT COUNT(*) FROM banned_word WHERE word = #{word}")
    int countByWord(String word);
}
