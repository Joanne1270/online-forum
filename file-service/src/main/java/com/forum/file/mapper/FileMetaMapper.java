package com.forum.file.mapper;

import com.forum.file.entity.FileMeta;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface FileMetaMapper {

    @Insert("INSERT INTO file_meta(original_name, stored_name, url, uploader_id, size, mime) "
            + "VALUES(#{originalName}, #{storedName}, #{url}, #{uploaderId}, #{size}, #{mime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileMeta meta);
}
