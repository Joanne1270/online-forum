package com.forum.user.mapper;

import com.forum.user.entity.ProfileChangeRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProfileChangeRequestMapper {

    @Insert("INSERT INTO profile_change_request(user_id, field_type, old_value, new_value, status) "
            + "VALUES(#{userId}, #{fieldType}, #{oldValue}, #{newValue}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProfileChangeRequest request);

    @Select("SELECT COUNT(*) FROM profile_change_request WHERE user_id = #{userId} AND field_type = #{fieldType} "
            + "AND DATE(created_at) = CURDATE()")
    int countTodayByUserAndField(@Param("userId") Long userId, @Param("fieldType") String fieldType);

    @Select("SELECT * FROM profile_change_request WHERE user_id = #{userId} AND field_type = #{fieldType} AND status = 0 LIMIT 1")
    ProfileChangeRequest findPending(@Param("userId") Long userId, @Param("fieldType") String fieldType);

    @Select("SELECT * FROM profile_change_request WHERE status = 0 ORDER BY created_at ASC LIMIT #{offset}, #{size}")
    List<ProfileChangeRequest> listPending(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM profile_change_request WHERE status = 0")
    long countPending();

    @Select("SELECT * FROM profile_change_request WHERE id = #{id}")
    ProfileChangeRequest findById(Long id);

    @Update("UPDATE profile_change_request SET status = #{status}, handled_at = NOW() WHERE id = #{id} AND status = 0")
    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
