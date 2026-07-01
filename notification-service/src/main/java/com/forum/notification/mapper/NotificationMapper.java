package com.forum.notification.mapper;

import com.forum.notification.entity.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO notification(user_id, type, ref_id, ref_type, post_id, from_user_id, content, read_flag) "
            + "VALUES(#{userId}, #{type}, #{refId}, #{refType}, #{postId}, #{fromUserId}, #{content}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    @Select("SELECT * FROM notification WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<Notification> listByUser(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    @Select("<script>SELECT * FROM notification WHERE user_id = #{userId} "
            + "<if test='types != null and types.size() &gt; 0'> AND type IN "
            + "<foreach collection='types' item='t' open='(' separator=',' close=')'>#{t}</foreach></if> "
            + "ORDER BY created_at DESC LIMIT #{offset}, #{size}</script>")
    List<Notification> listByUserAndTypes(@Param("userId") Long userId,
                                          @Param("types") List<String> types,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    @Select("<script>SELECT COUNT(*) FROM notification WHERE user_id = #{userId} "
            + "<if test='types != null and types.size() &gt; 0'> AND type IN "
            + "<foreach collection='types' item='t' open='(' separator=',' close=')'>#{t}</foreach></if></script>")
    long countByUserAndTypes(@Param("userId") Long userId, @Param("types") List<String> types);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId}")
    long countByUser(Long userId);

    @Select("<script>SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND read_flag = 0 "
            + "<if test='types != null and types.size() &gt; 0'> AND type IN "
            + "<foreach collection='types' item='t' open='(' separator=',' close=')'>#{t}</foreach></if></script>")
    long countUnreadByTypes(@Param("userId") Long userId, @Param("types") List<String> types);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND read_flag = 0")
    long countUnread(Long userId);

    @Update("UPDATE notification SET read_flag = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE notification SET read_flag = 1 WHERE user_id = #{userId}")
    int markAllRead(Long userId);

    @Update("<script>UPDATE notification SET read_flag = 1 WHERE user_id = #{userId} AND read_flag = 0 "
            + "<if test='types != null and types.size() &gt; 0'> AND type IN "
            + "<foreach collection='types' item='t' open='(' separator=',' close=')'>#{t}</foreach></if></script>")
    int markAllReadByTypes(@Param("userId") Long userId, @Param("types") List<String> types);
}
