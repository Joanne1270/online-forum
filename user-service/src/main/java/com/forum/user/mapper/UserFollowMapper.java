package com.forum.user.mapper;

import com.forum.user.dto.FollowUserVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserFollowMapper {

    @Insert("INSERT INTO user_follow(follower_id, followee_id) VALUES(#{followerId}, #{followeeId})")
    int insert(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Delete("DELETE FROM user_follow WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int delete(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int exists(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT u.id, u.nickname, u.avatar, u.bio, f.created_at AS followed_at "
            + "FROM user_follow f INNER JOIN user u ON u.id = f.followee_id "
            + "WHERE f.follower_id = #{followerId} AND u.role <> 'ADMIN' AND u.status <> 2 "
            + "ORDER BY f.created_at DESC")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "nickname", property = "nickname"),
            @Result(column = "avatar", property = "avatar"),
            @Result(column = "bio", property = "bio"),
            @Result(column = "followed_at", property = "followedAt")
    })
    List<FollowUserVO> listFollowing(Long followerId);

    @Select("SELECT followee_id FROM user_follow WHERE follower_id = #{followerId}")
    List<Long> listFollowingIds(Long followerId);

    @Select("SELECT u.id, u.nickname, u.avatar, u.bio, f.created_at AS followed_at "
            + "FROM user_follow f INNER JOIN user u ON u.id = f.follower_id "
            + "WHERE f.followee_id = #{followeeId} AND u.role <> 'ADMIN' AND u.status <> 2 "
            + "ORDER BY f.created_at DESC")
    @Results({
            @Result(column = "id", property = "id"),
            @Result(column = "nickname", property = "nickname"),
            @Result(column = "avatar", property = "avatar"),
            @Result(column = "bio", property = "bio"),
            @Result(column = "followed_at", property = "followedAt")
    })
    List<FollowUserVO> listFollowers(Long followeeId);

    @Select("SELECT COUNT(*) FROM user_follow f INNER JOIN user u ON u.id = f.followee_id "
            + "WHERE f.follower_id = #{followerId} AND u.role <> 'ADMIN' AND u.status <> 2")
    int countFollowing(Long followerId);

    @Select("SELECT COUNT(*) FROM user_follow f INNER JOIN user u ON u.id = f.follower_id "
            + "WHERE f.followee_id = #{followeeId} AND u.role <> 'ADMIN' AND u.status <> 2")
    int countFollowers(Long followeeId);

    @Delete("DELETE FROM user_follow WHERE follower_id = #{userId} OR followee_id = #{userId}")
    int deleteByUser(@Param("userId") Long userId);
}
