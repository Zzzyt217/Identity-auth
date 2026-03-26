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
 * AI 智能风控 - 风险引擎（行为特征 + 规则评分 + 可选 Python 模型 + 贝叶斯后验更新）
 * <p>
 * 最小落地：对非匿名用户，在规则/模型得到行为评分后，将当前窗口视为一次证据 E，
 * 用 P(E|Risk)、P(E|¬Risk) 与历史后验作为先验，按贝叶斯公式更新 P(Risk|E)，
 * 并以更新后的后验概率映射低/中/高风险（与拦截逻辑一致）。
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

    /** 分数 >= 此值视为中高风险（规则基线，匿名用户仅用规则） */
    private static final double THRESHOLD_HIGH = 80;
    /** 分数 >= 此值视为存在风险（中风险及以上） */
    private static final double THRESHOLD_RISK = 60;

    /** 后验 >= 此值判为高风险 */
    private static final double POSTERIOR_THRESHOLD_HIGH = 0.72;
    /**
     * 后验 >= 此值判为中风险（调低后：在默认先验 0.15 下，单次行为分约 80 即可映射为「中」，
     * 避免出现“规则已到中档强度却要 ~90 分才显示中”的滞后；与下方 {@link #maxRiskLevel} 一起保证不低于规则结论）。
     */
    private static final double POSTERIOR_THRESHOLD_RISK = 0.35;

    @Value("${ai.risk.model.enabled:false}")
    private boolean modelEnabled;
    @Value("${ai.risk.model.url:http://localhost:5000/predict}")
    private String modelUrl;
    @Value("${ai.risk.model.timeoutMs:2000}")
    private int modelTimeoutMs;

    @Resource
    private BehaviorRecordService behaviorRecordService;

    @Resource
    private BayesianRiskPosteriorStore bayesianRiskPosteriorStore;

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

        double roundedScore = Math.round(score * 10) / 10.0;

        boolean modelCalled = false;
        String modelLevelHint = null;
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
                    modelLevelHint = String.valueOf(resp.get("risk_level"));
                }
            } catch (Exception ignored) {
                // 模型不可用则仅规则 + 贝叶斯
            }
        }

        boolean modelStrongRisk = modelCalled && modelLevelHint != null
                && ("高".equals(modelLevelHint) || "中".equals(modelLevelHint));

        String riskLevel;
        boolean riskDetected;
        String message;
        if (modelStrongRisk) {
            riskLevel = modelLevelHint;
            riskDetected = true;
            message = "检测到短时间多次链上验证或身份查询，请确认是否为本人操作。";
        } else if (score >= THRESHOLD_HIGH) {
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

        // 匿名：保持原规则/模型结论，不做后验更新（避免所有匿名用户共享一条后验）
        if (userId == null || "anonymous".equals(userId)) {
            return new RiskResult(riskDetected, riskLevel, roundedScore, message, source, null);
        }

        // 登录用户：用当前评分构造似然，贝叶斯更新后验并映射等级
        double pEGivenR = likelihoodRisk(score, modelStrongRisk);
        double pEGivenNotR = likelihoodNotRisk(score, modelStrongRisk);
        double prior = bayesianRiskPosteriorStore.getPriorForUpdate(userId);
        double posterior = bayesPosterior(pEGivenR, pEGivenNotR, prior);
        bayesianRiskPosteriorStore.setPosterior(userId, posterior);

        String finalLevel;
        boolean finalDetected;
        if (posterior >= POSTERIOR_THRESHOLD_HIGH) {
            finalLevel = "高";
            finalDetected = true;
        } else if (posterior >= POSTERIOR_THRESHOLD_RISK) {
            finalLevel = "中";
            finalDetected = true;
        } else {
            finalLevel = "低";
            finalDetected = false;
        }

        // 模型已给出「高/中」时，不得因后验映射略低而弱于模型结论（与原先立即升高风险语义一致）
        if (modelStrongRisk) {
            if ("高".equals(modelLevelHint)) {
                finalLevel = "高";
                finalDetected = true;
            } else if ("中".equals(modelLevelHint) && "低".equals(finalLevel)) {
                finalLevel = "中";
                finalDetected = true;
            }
        }

        // 与规则/模型结论取高：避免后验映射把「规则已判高（如 score≥80）」压成「中」
        finalLevel = maxRiskLevel(finalLevel, riskLevel);
        finalDetected = !"低".equals(finalLevel);

        String msg = message;
        if (finalDetected && !riskDetected) {
            msg = "综合行为证据与历史风险后验，当前评估为仍存在一定风险，请确认是否为本人操作。";
        } else if (!finalDetected && riskDetected) {
            msg = "综合行为证据与历史风险后验，当前评估已回落至正常范围。";
        }

        return new RiskResult(finalDetected, finalLevel, roundedScore, msg, source, posterior);
    }

    /** 在 Risk 成立条件下观察到当前强度评分的可能性（归一化到合理区间） */
    private static double likelihoodRisk(double score, boolean modelStrongRisk) {
        double s = Math.min(1.0, Math.max(0.0, score / 100.0));
        double p = 0.15 + 0.80 * s;
        if (modelStrongRisk) {
            p = Math.min(0.95, p + 0.12);
        }
        return clamp(p, 0.08, 0.95);
    }

    /** 在 Risk 不成立条件下观察到当前强度评分的可能性 */
    private static double likelihoodNotRisk(double score, boolean modelStrongRisk) {
        double s = Math.min(1.0, Math.max(0.0, score / 100.0));
        double p = 0.85 - 0.75 * s;
        if (modelStrongRisk) {
            p = Math.max(0.08, p - 0.10);
        }
        return clamp(p, 0.08, 0.92);
    }

    private static double bayesPosterior(double pEGivenR, double pEGivenNotR, double prior) {
        double pR = clamp(prior, 0.0, 1.0);
        double pNotR = 1.0 - pR;
        double pE = pEGivenR * pR + pEGivenNotR * pNotR;
        if (pE <= 1e-12) {
            return pR;
        }
        return clamp((pEGivenR * pR) / pE, 0.0, 1.0);
    }

    private static double clamp(double x, double lo, double hi) {
        if (x < lo) return lo;
        if (x > hi) return hi;
        return x;
    }

    private static int riskLevelRank(String level) {
        if ("高".equals(level)) return 2;
        if ("中".equals(level)) return 1;
        return 0;
    }

    /** 取较高的风险等级（低 &lt; 中 &lt; 高） */
    private static String maxRiskLevel(String a, String b) {
        if (a == null) return b != null ? b : "低";
        if (b == null) return a;
        return riskLevelRank(a) >= riskLevelRank(b) ? a : b;
    }
}
