package com.test.risk;

import java.util.Objects;

/**
 * AI 智能风控 - 行为记录（单条）
 * 用于行为建模与风险引擎特征计算。
 */
public class BehaviorRecord {

    private final String userId;
    private final String actionType;
    private final long timestamp;

    public BehaviorRecord(String userId, String actionType, long timestamp) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.actionType = Objects.requireNonNull(actionType, "actionType");
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getActionType() {
        return actionType;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
