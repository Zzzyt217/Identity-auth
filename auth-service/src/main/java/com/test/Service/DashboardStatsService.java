package com.test.Service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 首页仪表盘统计：今日验证次数、今日中高风险事件数。
 * 按自然日计数，每次链上验证请求 +1 验证次数，每次出现中高风险或拒绝操作 +1 中高风险事件。
 */
@Service
public class DashboardStatsService {

    private final ConcurrentHashMap<String, AtomicInteger> verifyCountByDay = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> highRiskCountByDay = new ConcurrentHashMap<>();

    private static String todayKey() {
        return LocalDate.now().toString();
    }

    /** 每次链上验证（身份验证或操作认证）被请求时调用，计数 +1 */
    public void incrementVerifyCount() {
        verifyCountByDay.computeIfAbsent(todayKey(), k -> new AtomicInteger(0)).incrementAndGet();
    }

    /** 每次风险等级为「高」（中高风险）或返回拒绝操作时调用，计数 +1 */
    public void incrementHighRiskCount() {
        highRiskCountByDay.computeIfAbsent(todayKey(), k -> new AtomicInteger(0)).incrementAndGet();
    }

    public int getTodayVerifyCount() {
        return verifyCountByDay.getOrDefault(todayKey(), new AtomicInteger(0)).get();
    }

    public int getTodayHighRiskCount() {
        return highRiskCountByDay.getOrDefault(todayKey(), new AtomicInteger(0)).get();
    }
}
