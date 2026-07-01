package com.forum.user.mapper;

import com.forum.user.entity.PrivateMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PrivateMessageMapper {

    @Insert("INSERT INTO private_message(sender_id, receiver_id, content) VALUES(#{senderId}, #{receiverId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PrivateMessage message);

    @Select("SELECT pm.id, pm.sender_id, pm.receiver_id, pm.content, pm.read_flag, pm.created_at, "
            + "CASE WHEN pm.sender_id = #{userId} THEN pm.receiver_id ELSE pm.sender_id END AS peer_id "
            + "FROM private_message pm "
            + "INNER JOIN ( "
            + "  SELECT MAX(id) AS max_id FROM private_message "
            + "  WHERE sender_id = #{userId} OR receiver_id = #{userId} "
            + "  GROUP BY CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END "
            + ") t ON pm.id = t.max_id "
            + "ORDER BY pm.created_at DESC")
    @Results({
            @Result(column = "sender_id", property = "senderId"),
            @Result(column = "receiver_id", property = "receiverId"),
            @Result(column = "read_flag", property = "readFlag"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "peer_id", property = "peerId")
    })
    List<PrivateMessage> listLatestConversations(Long userId);

    @Select("SELECT COUNT(*) FROM private_message "
            + "WHERE receiver_id = #{userId} AND sender_id = #{peerId} AND read_flag = 0")
    long countUnreadWithPeer(@Param("userId") Long userId, @Param("peerId") Long peerId);

    @Select("SELECT COUNT(*) FROM private_message WHERE receiver_id = #{userId} AND read_flag = 0")
    long countUnread(Long userId);

    @Select("SELECT * FROM private_message "
            + "WHERE (sender_id = #{userId} AND receiver_id = #{peerId}) "
            + "OR (sender_id = #{peerId} AND receiver_id = #{userId}) "
            + "ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    @Results({
            @Result(column = "sender_id", property = "senderId"),
            @Result(column = "receiver_id", property = "receiverId"),
            @Result(column = "read_flag", property = "readFlag"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<PrivateMessage> listWithPeer(@Param("userId") Long userId,
                                      @Param("peerId") Long peerId,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    @Select("SELECT COUNT(*) FROM private_message "
            + "WHERE (sender_id = #{userId} AND receiver_id = #{peerId}) "
            + "OR (sender_id = #{peerId} AND receiver_id = #{userId})")
    long countWithPeer(@Param("userId") Long userId, @Param("peerId") Long peerId);

    @Update("UPDATE private_message SET read_flag = 1 "
            + "WHERE receiver_id = #{userId} AND sender_id = #{peerId} AND read_flag = 0")
    int markReadWithPeer(@Param("userId") Long userId, @Param("peerId") Long peerId);
}
