package com.test.risk;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 贝叶斯后验风险状态：按用户维度保存最近一次 P(Risk)（风险状态为真）的后验概率。
 * 与 {@link AiRiskEngine} 配合：每次评估用当前行为证据做一次贝叶斯更新，下一轮的「先验」取本轮后验。
 */
@Service
public class BayesianRiskPosteriorStore {

    /** 新用户默认先验 P(Risk) */
    private static final double DEFAULT_PRIOR = 0.15;

    private final ConcurrentHashMap<String, Double> posteriorByUser = new ConcurrentHashMap<>();

    public double getPriorForUpdate(String userId) {
        if (userId == null || userId.isEmpty()) {
            return DEFAULT_PRIOR;
        }
        return posteriorByUser.getOrDefault(userId, DEFAULT_PRIOR);
    }

    public void setPosterior(String userId, double posterior) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        double p = posterior;
        if (p < 0.0) p = 0.0;
        if (p > 1.0) p = 1.0;
        posteriorByUser.put(userId, p);
    }
}
