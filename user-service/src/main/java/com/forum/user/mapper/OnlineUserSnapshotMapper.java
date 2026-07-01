package com.forum.user.mapper;

import com.forum.user.entity.OnlineUserSnapshot;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OnlineUserSnapshotMapper {

    @Insert("INSERT INTO online_user_snapshot(granularity, snapshot_at, online_count) "
            + "VALUES(#{granularity}, #{snapshotAt}, #{onlineCount}) "
            + "ON DUPLICATE KEY UPDATE online_count = #{onlineCount}, updated_at = NOW()")
    int upsert(@Param("granularity") String granularity,
               @Param("snapshotAt") LocalDateTime snapshotAt,
               @Param("onlineCount") int onlineCount);

    @Select("SELECT snapshot_at, online_count FROM online_user_snapshot "
            + "WHERE granularity = #{granularity} AND snapshot_at >= #{start} AND snapshot_at <= #{end} "
            + "ORDER BY snapshot_at ASC")
    @Results({
            @Result(column = "snapshot_at", property = "snapshotAt"),
            @Result(column = "online_count", property = "onlineCount")
    })
    List<OnlineUserSnapshot> listRange(@Param("granularity") String granularity,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}
