package com.forum.user.service;

import com.forum.user.dto.OnlineStatPointVO;
import com.forum.user.dto.OnlineStatsVO;
import com.forum.user.entity.OnlineUserSnapshot;
import com.forum.user.mapper.OnlineUserSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private static final String REDIS_KEY_ACTIVE = "online:active";
    private static final long ONLINE_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final String GRANULARITY_HOUR = "HOUR";
    private static final String GRANULARITY_DAY = "DAY";
    private static final String GRANULARITY_HALF_MONTH = "HALF_MONTH";

    private final StringRedisTemplate redisTemplate;
    private final OnlineUserSnapshotMapper snapshotMapper;

    public void heartbeat(Long userId) {
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(REDIS_KEY_ACTIVE, String.valueOf(userId), now);
        redisTemplate.opsForZSet().removeRangeByScore(REDIS_KEY_ACTIVE, 0, now - ONLINE_TIMEOUT_MS);
    }

    public int countOnline() {
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().removeRangeByScore(REDIS_KEY_ACTIVE, 0, now - ONLINE_TIMEOUT_MS);
        Long count = redisTemplate.opsForZSet().count(REDIS_KEY_ACTIVE, now - ONLINE_TIMEOUT_MS, now);
        return count == null ? 0 : count.intValue();
    }

    @Scheduled(fixedRate = 60000)
    public void recordSnapshot() {
        int online = countOnline();
        LocalDateTime now = LocalDateTime.now();
        snapshotMapper.upsert(GRANULARITY_HOUR, now.truncatedTo(ChronoUnit.HOURS), online);
        snapshotMapper.upsert(GRANULARITY_DAY, now.toLocalDate().atStartOfDay(), online);
        snapshotMapper.upsert(GRANULARITY_HALF_MONTH, resolveHalfMonthStart(now), online);
    }

    public OnlineStatsVO getStats(String range) {
        int currentOnline = countOnline();
        OnlineStatsVO vo = new OnlineStatsVO();
        vo.setCurrentOnline(currentOnline);
        if ("month".equalsIgnoreCase(range)) {
            vo.setPoints(buildMonthPoints(currentOnline));
        } else if ("year".equalsIgnoreCase(range)) {
            vo.setPoints(buildYearPoints(currentOnline));
        } else {
            vo.setPoints(buildDayPoints(currentOnline));
        }
        return vo;
    }

    private List<OnlineStatPointVO> buildDayPoints(int currentOnline) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.toLocalDate().atStartOfDay();
        LocalDateTime end = now.truncatedTo(ChronoUnit.HOURS);
        Map<LocalDateTime, Integer> stored = toCountMap(
                snapshotMapper.listRange(GRANULARITY_HOUR, start, end));
        List<OnlineStatPointVO> points = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        for (LocalDateTime cursor = start; !cursor.isAfter(end); cursor = cursor.plusHours(1)) {
            OnlineStatPointVO point = new OnlineStatPointVO();
            point.setTime(cursor);
            point.setLabel(cursor.format(fmt));
            boolean isCurrentHour = cursor.equals(end);
            point.setCount(isCurrentHour ? currentOnline : stored.getOrDefault(cursor, 0));
            points.add(point);
        }
        return points;
    }

    private List<OnlineStatPointVO> buildMonthPoints(int currentOnline) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = today.atStartOfDay();
        Map<LocalDateTime, Integer> stored = toCountMap(
                snapshotMapper.listRange(GRANULARITY_DAY, start, end));
        List<OnlineStatPointVO> points = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");
        for (LocalDate cursor = start.toLocalDate(); !cursor.isAfter(today); cursor = cursor.plusDays(1)) {
            LocalDateTime bucket = cursor.atStartOfDay();
            OnlineStatPointVO point = new OnlineStatPointVO();
            point.setTime(bucket);
            point.setLabel(cursor.format(fmt));
            boolean isToday = cursor.equals(today);
            point.setCount(isToday ? currentOnline : stored.getOrDefault(bucket, 0));
            points.add(point);
        }
        return points;
    }

    private List<OnlineStatPointVO> buildYearPoints(int currentOnline) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
        LocalDateTime end = resolveHalfMonthStart(today.atStartOfDay());
        Map<LocalDateTime, Integer> stored = toCountMap(
                snapshotMapper.listRange(GRANULARITY_HALF_MONTH, start, end));
        List<OnlineStatPointVO> points = new ArrayList<>();
        LocalDateTime cursor = start;
        while (!cursor.isAfter(end)) {
            OnlineStatPointVO point = new OnlineStatPointVO();
            point.setTime(cursor);
            point.setLabel(formatHalfMonthLabel(cursor));
            boolean isCurrent = cursor.equals(end);
            point.setCount(isCurrent ? currentOnline : stored.getOrDefault(cursor, 0));
            points.add(point);
            cursor = nextHalfMonth(cursor);
        }
        return points;
    }

    private Map<LocalDateTime, Integer> toCountMap(List<OnlineUserSnapshot> snapshots) {
        Map<LocalDateTime, Integer> map = new HashMap<>();
        if (snapshots == null) {
            return map;
        }
        for (OnlineUserSnapshot snapshot : snapshots) {
            if (snapshot.getSnapshotAt() != null && snapshot.getOnlineCount() != null) {
                map.put(snapshot.getSnapshotAt(), snapshot.getOnlineCount());
            }
        }
        return map;
    }

    private LocalDateTime resolveHalfMonthStart(LocalDateTime time) {
        LocalDate date = time.toLocalDate();
        int day = date.getDayOfMonth() <= 15 ? 1 : 16;
        return LocalDate.of(date.getYear(), date.getMonth(), day).atStartOfDay();
    }

    private LocalDateTime nextHalfMonth(LocalDateTime bucket) {
        LocalDate date = bucket.toLocalDate();
        if (date.getDayOfMonth() == 1) {
            return LocalDate.of(date.getYear(), date.getMonth(), 16).atStartOfDay();
        }
        YearMonth nextMonth = YearMonth.from(date).plusMonths(1);
        return nextMonth.atDay(1).atStartOfDay();
    }

    private String formatHalfMonthLabel(LocalDateTime bucket) {
        int month = bucket.getMonthValue();
        return bucket.getDayOfMonth() == 1 ? month + "月上" : month + "月下";
    }
}
