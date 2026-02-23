package com.test.risk;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 风险升级：中风险预警后，若再频繁操作达到阈值，则升级为中高风险并拒绝后续查询/认证操作。
 * 使用 Session 记录：是否已中风险预警、预警后操作次数；升级为高风险后同时写入按用户维度的持久集合，不随登出或时间衰减。
 */
@Service
public class RiskEscalationService {

    private static final String SESSION_RISK_BLOCKED = "riskBlocked";
    private static final String SESSION_RISK_WARNED_MEDIUM = "riskWarnedMedium";
    private static final String SESSION_RISK_OPS_AFTER_WARNING = "riskOpsAfterWarning";
    private static final String SESSION_USER = "user";
    /** 中风险预警后再操作此次数即升级为中高风险并拒绝 */
    private static final int THRESHOLD_OPS_TO_BLOCK = 3;

    /** 已升级为高风险（拒绝）的用户 ID 集合，不随 Session 登出或时间衰减，仅内存持久 */
    private final Set<String> blockedUserIds = ConcurrentHashMap.newKeySet();

    /**
     * 是否已因「中风险预警后仍频繁操作」被拒绝（后续查询/认证应直接返回拒绝）。
     * 先查 Session，再查按用户维度的持久集合，保证升级后不随时间或登出而恢复。
     */
    public boolean isBlocked(HttpSession session) {
        if (session == null) return false;
        if (Boolean.TRUE.equals(session.getAttribute(SESSION_RISK_BLOCKED))) return true;
        String userId = (String) session.getAttribute(SESSION_USER);
        if (userId == null) userId = "anonymous";
        return blockedUserIds.contains(userId);
    }

    /**
     * 本请求为一次查询或认证操作后调用：若本次风险为「中」则标记已预警；
     * 若此前已预警则本请求计为「预警后又一次操作」，累加次数，达到 {@link #THRESHOLD_OPS_TO_BLOCK} 则置为拒绝。
     */
    public void applyAfterRequest(HttpSession session, RiskResult riskResult) {
        if (session == null || riskResult == null) return;
        if (isBlocked(session)) return;

        boolean wasWarned = Boolean.TRUE.equals(session.getAttribute(SESSION_RISK_WARNED_MEDIUM));
        if ("中".equals(riskResult.getRiskLevel())) {
            session.setAttribute(SESSION_RISK_WARNED_MEDIUM, true);
        }
        if (wasWarned) {
            Integer count = (Integer) session.getAttribute(SESSION_RISK_OPS_AFTER_WARNING);
            if (count == null) count = 0;
            count++;
            session.setAttribute(SESSION_RISK_OPS_AFTER_WARNING, count);
            if (count >= THRESHOLD_OPS_TO_BLOCK) {
                session.setAttribute(SESSION_RISK_BLOCKED, true);
                String userId = (String) session.getAttribute(SESSION_USER);
                if (userId == null) userId = "anonymous";
                blockedUserIds.add(userId);
            }
        }
    }

    /**
     * 构造「拒绝操作」的 API 响应（用于查询/认证接口在已拒绝时直接返回）。
     */
    public Map<String, Object> createRefuseResponse() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("riskBlocked", true);
        result.put("message", "拒绝操作");
        Map<String, Object> risk = new HashMap<>();
        risk.put("riskDetected", true);
        risk.put("riskLevel", "高");
        risk.put("message", "因多次预警后仍频繁操作，已升级为高风险，拒绝本次操作。");
        risk.put("riskSource", "rule");
        risk.put("riskBlocked", true);
        result.put("risk", risk);
        return result;
    }

    /**
     * 清除当前 Session 的风险升级状态（切换账号或登出时调用，避免新用户继承上一用户的中高风险/拒绝状态）。
     * 不清除 blockedUserIds，故已升级为高风险的账号在重新登录后仍保持拒绝，不随时间衰减。
     */
    public void clearSessionState(HttpSession session) {
        if (session == null) return;
        session.removeAttribute(SESSION_RISK_BLOCKED);
        session.removeAttribute(SESSION_RISK_WARNED_MEDIUM);
        session.removeAttribute(SESSION_RISK_OPS_AFTER_WARNING);
    }
}
