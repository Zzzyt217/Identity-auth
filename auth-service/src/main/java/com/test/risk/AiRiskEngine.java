package com.test.risk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能风控 - 风险引擎（行为特征 + 规则评分 + 风险等级）
 * 不训练模型，基于近期行为统计与可解释的评分函数输出风险结果。
 */
@Service
public class AiRiskEngine {

    private static final long ONE_MINUTE_MS = 60 * 1000L;
    private static final long FIVE_MINUTES_MS = 5 * 60 * 1000L;

    /** 链上验证权重（1 分钟内） */
    private static final double WEIGHT_VERIFY_1M = 2.0;
    /** 身份查询权重（1 分钟内） */
    private static final double WEIGHT_QUERY_1M = 1.0;
    /** 链上验证权重（5 分钟内） */
    private static final double WEIGHT_VERIFY_5M = 0.5;
    /** 身份查询权重（5 分钟内） */
    private static final double WEIGHT_QUERY_5M = 0.3;

    /** 分数 >= 此值视为高风险 */
    private static final double THRESHOLD_HIGH = 80;
    /** 分数 >= 此值视为存在风险（中风险及以上），与“阈值 60”一致 */
    private static final double THRESHOLD_RISK = 60;

    @Value("${ai.risk.model.enabled:false}")
    private boolean modelEnabled;
    @Value("${ai.risk.model.url:http://localhost:5000/predict}")
    private String modelUrl;
    @Value("${ai.risk.model.timeoutMs:2000}")
    private int modelTimeoutMs;

    @Resource
    private BehaviorRecordService behaviorRecordService;

    /**
     * 评估当前用户的风险（供 /api/risk/check 及后续弹窗使用）
     *
     * @param userId 当前用户标识（Session 的 user 或 "anonymous"）
     * @return 风险结果，不会为 null
     */
    public RiskResult evaluateRisk(String userId) {
        long now = System.currentTimeMillis();
        long since5m = now - FIVE_MINUTES_MS;
        long since1m = now - ONE_MINUTE_MS;

        List<BehaviorRecord> records = behaviorRecordService.getRecentRecords(userId, since5m);
        if (records.isEmpty()) {
            return new RiskResult(false, "低", 0.0, "近期无相关行为记录，风险较低。", "rule");
        }

        int countVerify1m = 0, countQuery1m = 0, countVerify5m = 0, countQuery5m = 0;
        for (BehaviorRecord r : records) {
            long ts = r.getTimestamp();
            String type = r.getActionType();
            if (BehaviorRecordService.ACTION_CHAIN_VERIFY.equals(type)) {
                if (ts >= since1m) countVerify1m++;
                countVerify5m++;
            } else if (BehaviorRecordService.ACTION_IDENTITY_QUERY.equals(type)) {
                if (ts >= since1m) countQuery1m++;
                countQuery5m++;
            }
        }

        double score = WEIGHT_VERIFY_1M * countVerify1m
                + WEIGHT_QUERY_1M * countQuery1m
                + WEIGHT_VERIFY_5M * countVerify5m
                + WEIGHT_QUERY_5M * countQuery5m;

        String riskLevel;
        boolean riskDetected;
        String message;
        boolean modelCalled = false;
        if (modelEnabled && modelUrl != null && !modelUrl.isEmpty()) {
            try {
                RestTemplate rest = new RestTemplate();
                org.springframework.http.client.SimpleClientHttpRequestFactory f =
                        new org.springframework.http.client.SimpleClientHttpRequestFactory();
                f.setConnectTimeout(modelTimeoutMs);
                f.setReadTimeout(modelTimeoutMs);
                rest.setRequestFactory(f);
                Map<String, Object> body = new HashMap<>();
                body.put("count_verify_1m", countVerify1m);
                body.put("count_query_1m", countQuery1m);
                body.put("count_verify_5m", countVerify5m);
                body.put("count_query_5m", countQuery5m);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                @SuppressWarnings("unchecked")
                Map<String, Object> resp = rest.postForObject(modelUrl, new HttpEntity<>(body, headers), Map.class);
                if (resp != null && resp.containsKey("risk_level")) {
                    modelCalled = true;
                    String modelLevel = String.valueOf(resp.get("risk_level"));
                    if ("高".equals(modelLevel) || "中".equals(modelLevel)) {
                        riskLevel = modelLevel;
                        riskDetected = true;
                        message = "检测到短时间多次链上验证或身份查询，请确认是否为本人操作。";
                        return new RiskResult(riskDetected, riskLevel, Math.round(score * 10) / 10.0, message, "model");
                    }
                    // 模型返回「低」时，仍用规则阈值 60/80 决定等级与是否弹窗，但来源显示为「AI 异常检测模型」
                }
            } catch (Exception ignored) {
                // 模型服务不可用或超时，降级为规则评分
            }
        }

        if (score >= THRESHOLD_HIGH) {
            riskLevel = "高";
            riskDetected = true;
            message = "检测到短时间多次链上验证或身份查询，请确认是否为本人操作。";
        } else if (score >= THRESHOLD_RISK) {
            riskLevel = "中";
            riskDetected = true;
            message = "检测到短时间多次链上验证或身份查询，请确认是否为本人操作。";
        } else {
            riskLevel = "低";
            riskDetected = false;
            message = "当前行为在正常范围内。";
        }

        String source = modelCalled ? "model" : "rule";
        return new RiskResult(riskDetected, riskLevel, Math.round(score * 10) / 10.0, message, source);
    }
}
