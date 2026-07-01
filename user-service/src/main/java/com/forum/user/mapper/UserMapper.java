package com.forum.user.mapper;

import com.forum.user.entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User findByPhone(String phone);

    @Select("SELECT * FROM user WHERE nickname = #{nickname}")
    User findByNickname(String nickname);

    @Select("SELECT * FROM user WHERE email <> '' AND LOWER(email) = LOWER(#{email})")
    User findByEmail(String email);

    @Select("<script>SELECT * FROM user WHERE nickname IN "
            + "<foreach collection='nicknames' item='n' open='(' separator=',' close=')'>#{n}</foreach></script>")
    List<User> findByNicknames(@Param("nicknames") List<String> nicknames);

    @Select("SELECT * FROM user ORDER BY id DESC LIMIT #{offset}, #{size}")
    List<User> listAll(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM user")
    long countAll();

    @Insert("INSERT INTO user(phone, password_hash, nickname, avatar, email, bio, role, status) "
            + "VALUES(#{phone}, #{passwordHash}, #{nickname}, #{avatar}, #{email}, #{bio}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET nickname=#{nickname}, avatar=#{avatar}, email=#{email}, bio=#{bio}, "
            + "gender=#{gender}, birth_month=#{birthMonth} WHERE id=#{id}")
    int updateProfile(User user);

    @Update("UPDATE user SET privacy_posts=#{privacyPosts}, privacy_favorites=#{privacyFavorites}, privacy_replies=#{privacyReplies}, "
            + "privacy_following=#{privacyFollowing}, privacy_followers=#{privacyFollowers} WHERE id=#{id}")
    int updatePrivacy(@Param("id") Long id,
                      @Param("privacyPosts") Integer privacyPosts,
                      @Param("privacyFavorites") Integer privacyFavorites,
                      @Param("privacyReplies") Integer privacyReplies,
                      @Param("privacyFollowing") Integer privacyFollowing,
                      @Param("privacyFollowers") Integer privacyFollowers);

    @Update("UPDATE user SET status=#{status}, banned_until=#{bannedUntil} WHERE id=#{id}")
    int updateBanStatus(@Param("id") Long id, @Param("status") Integer status,
                        @Param("bannedUntil") LocalDateTime bannedUntil);

    @Update("UPDATE user SET role=#{role} WHERE id=#{id}")
    int updateRole(@Param("id") Long id, @Param("role") String role);

    @Update("UPDATE user SET status=#{status}, phone=#{phone}, nickname=#{nickname}, avatar='', email='', bio='', "
            + "gender='SECRET', birth_month=NULL, "
            + "banned_until=NULL, password_hash=#{passwordHash} WHERE id=#{id}")
    int deactivateAccount(@Param("id") Long id,
                          @Param("status") Integer status,
                          @Param("phone") String phone,
                          @Param("nickname") String nickname,
                          @Param("passwordHash") String passwordHash);

    @Update("UPDATE user SET password_hash=#{passwordHash} WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Select("SELECT * FROM user WHERE nickname LIKE CONCAT('%', #{keyword}, '%') LIMIT #{limit}")
    List<User> search(@Param("keyword") String keyword, @Param("limit") int limit);
}
