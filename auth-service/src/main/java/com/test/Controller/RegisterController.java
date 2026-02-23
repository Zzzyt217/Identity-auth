package com.test.Controller;

import com.test.Entity.Identity;
import com.test.Service.IdentityService;
import com.test.Service.AuditLogService;
import com.test.Service.DashboardStatsService;
import com.test.risk.RiskEscalationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@Controller
public class RegisterController {

    private static final String SESSION_ADMIN = "admin";

    @Resource
    private IdentityService identityService;
    @Resource
    private AuditLogService auditLogService;
    @Resource
    private DashboardStatsService dashboardStatsService;
    @Resource
    private RiskEscalationService riskEscalationService;

    /** 首页：根据是否管理员/测试用户登录展示权限管理中心及登录区，并同步仪表盘统计 */
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        model.addAttribute("isAdmin", session.getAttribute(SESSION_ADMIN) != null);
        model.addAttribute("currentUser", session.getAttribute("user")); // user1 / user2 / user3 或 null
        model.addAttribute("didCount", identityService.count());
        long chainTxTotal = identityService.listAll().stream()
                .filter(i -> i.getChainTxHash() != null && !i.getChainTxHash().trim().isEmpty())
                .count();
        chainTxTotal += auditLogService.listAll().stream()
                .filter(l -> l.getChainTxHash() != null && !l.getChainTxHash().trim().isEmpty())
                .count();
        model.addAttribute("chainTxTotal", chainTxTotal);
        model.addAttribute("todayVerifyCount", dashboardStatsService != null ? dashboardStatsService.getTodayVerifyCount() : 0);
        model.addAttribute("highRiskCount", dashboardStatsService != null ? dashboardStatsService.getTodayHighRiskCount() : 0);
        return "index";
    }

    /** 管理员登录页 */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirect", required = false) String redirect, Model model) {
        model.addAttribute("redirect", redirect != null ? redirect : "/");
        model.addAttribute("testUserLogin", false);
        return "login";
    }

    /** 测试用户登录页（与管理员登录分开） */
    @GetMapping("/login/test")
    public String testUserLoginPage(@RequestParam(value = "redirect", required = false) String redirect, Model model) {
        model.addAttribute("redirect", redirect != null ? redirect : "/");
        model.addAttribute("testUserLogin", true);
        return "login";
    }

    /** 登出：清除 Session（管理员 + 测试用户 + 风险升级状态）并跳转首页 */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_ADMIN);
        session.removeAttribute("user");
        if (riskEscalationService != null) riskEscalationService.clearSessionState(session);
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/query")
    public String queryPage() {
        return "query";
    }

    /** 凭证页：展示 DID 及身份信息 */
    @GetMapping("/identity/credential/{id}")
    public String credentialPage(@PathVariable Long id, Model model) {
        Identity identity = identityService.getById(id);
        if (identity == null) {
            return "redirect:/query?notFound=1";
        }
        model.addAttribute("identity", identity);
        return "credential";
    }

    /** 权限管理页：仅管理员可访问 */
    @GetMapping("/admin/permissions")
    public String permissionsPage(HttpSession session) {
        if (session.getAttribute(SESSION_ADMIN) == null) {
            return "redirect:/login?redirect=/admin/permissions";
        }
        return "permissions";
    }

    /** 审计日志页：仅管理员可访问 */
    @GetMapping("/admin/audit")
    public String auditPage(HttpSession session) {
        if (session.getAttribute(SESSION_ADMIN) == null) {
            return "redirect:/login?redirect=/admin/audit";
        }
        return "audit";
    }

    /** AI 智能风控 - 风险报告页（调用 /api/risk/check 展示当前用户风险分析结果） */
    @GetMapping("/auth/risk-report")
    public String riskReportPage() {
        return "risk-report";
    }
}