package com.test.Controller;

import com.test.risk.AiRiskEngine;
import com.test.risk.RiskResult;
import com.test.risk.RiskEscalationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * AI 智能风控 - 风险检查 API
 * 对外暴露 GET /api/risk/check，供风险报告页及后续弹窗使用。
 */
@RestController
@RequestMapping("/api")
public class RiskController {

    @Resource
    private AiRiskEngine aiRiskEngine;
    @Resource
    private RiskEscalationService riskEscalationService;

    /** 升级为高风险后的固定展示分数（与规则阈值 80 对应） */
    private static final double ESCALATED_HIGH_SCORE = 80.0;

    /**
     * 获取当前用户的风险分析结果（从 Session 取用户，调用 AI 风险引擎）
     * 若已因「中风险预警后仍频繁操作」被拒绝，则直接返回高风险及对应分数，保证风险分析页与拒绝状态一致。
     * GET /api/risk/check
     */
    @GetMapping("/risk/check")
    public RiskResult checkRisk(HttpSession session) {
        String userId = session != null && session.getAttribute("user") != null
                ? (String) session.getAttribute("user") : "anonymous";
        if (riskEscalationService != null && riskEscalationService.isBlocked(session)) {
            return new RiskResult(
                    true,
                    "高",
                    ESCALATED_HIGH_SCORE,
                    "因多次预警后仍频繁操作，已升级为高风险，当前已拒绝查询/认证操作。",
                    "rule"
            );
        }
        return aiRiskEngine.evaluateRisk(userId);
    }
}
