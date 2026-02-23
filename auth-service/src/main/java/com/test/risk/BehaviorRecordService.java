package com.test.risk;

import org.springframework.stereotype.Service;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 智能风控 - 行为记录存储（内存）
 * 按用户保存近期行为，供风险引擎计算特征。仅保留最近一段时间/条数，避免无限增长。
 */
@Service
@ThreadSafe
public class BehaviorRecordService {

    /** 行为类型：链上验证 */
    public static final String ACTION_CHAIN_VERIFY = "CHAIN_VERIFY";
    /** 行为类型：身份查询 */
    public static final String ACTION_IDENTITY_QUERY = "IDENTITY_QUERY";

    /** 单用户最多保留条数 */
    private static final int MAX_RECORDS_PER_USER = 500;
    /** 只保留最近多少毫秒内的记录（5 分钟） */
    private static final long RETENTION_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, Deque<BehaviorRecord>> storage = new ConcurrentHashMap<>();

    /**
     * 记录一次行为（链上验证、身份查询等入口调用）
     *
     * @param userId     当前用户，可为 "anonymous"
     * @param actionType {@link #ACTION_CHAIN_VERIFY} 或 {@link #ACTION_IDENTITY_QUERY}
     * @param timestamp  操作时间（毫秒）
     */
    public void record(String userId, String actionType, long timestamp) {
        if (userId == null) {
            userId = "anonymous";
        }
        if (actionType == null || actionType.trim().isEmpty()) {
            return;
        }
        Deque<BehaviorRecord> queue = storage.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (queue) {
            queue.addLast(new BehaviorRecord(userId, actionType, timestamp));
            trim(queue, timestamp);
        }
    }

    /**
     * 获取某用户近期记录（供风险引擎计算特征用）
     * 仅返回仍在保留时间窗口内的记录。
     *
     * @param userId 用户标识
     * @param since  只返回该时间戳（毫秒）之后的记录
     * @return 按时间升序的记录列表，不会为 null
     */
    public List<BehaviorRecord> getRecentRecords(String userId, long since) {
        if (userId == null) {
            userId = "anonymous";
        }
        Deque<BehaviorRecord> queue = storage.get(userId);
        if (queue == null) {
            return Collections.emptyList();
        }
        synchronized (queue) {
            return queue.stream()
                    .filter(r -> r.getTimestamp() >= since)
                    .collect(Collectors.toList());
        }
    }

    private void trim(Deque<BehaviorRecord> queue, long now) {
        long cutoff = now - RETENTION_MS;
        while (queue.size() > MAX_RECORDS_PER_USER || (!queue.isEmpty() && queue.peekFirst().getTimestamp() < cutoff)) {
            if (queue.isEmpty()) break;
            queue.removeFirst();
        }
    }
}
