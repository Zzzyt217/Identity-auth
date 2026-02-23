package com.test.Controller;

import com.test.risk.RiskEscalationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员与测试用户登录分离：管理员仅 /api/login，测试用户仅 /api/login/test
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    @Value("${admin.username:admin}")
    private String adminUsername;
    @Value("${admin.password:123456}")
    private String adminPassword;

    @Resource
    private RiskEscalationService riskEscalationService;

    private static final String SESSION_ADMIN = "admin";
    private static final String SESSION_USER = "user";
    private static final String TEST_USER_PASSWORD = "123456";

    /** 仅接受管理员账号，拒绝测试用户 */
    @PostMapping("/login")
    public Map<String, Object> adminLogin(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        Map<String, Object> res = new HashMap<>();
        if (isTestUser(username)) {
            res.put("success", false);
            res.put("message", "请使用测试用户登录页登录");
            return res;
        }
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            session.removeAttribute(SESSION_USER);
            session.setAttribute(SESSION_ADMIN, true);
            session.setAttribute(SESSION_USER, "admin");
            if (riskEscalationService != null) riskEscalationService.clearSessionState(session);
            res.put("success", true);
            res.put("message", "登录成功");
            return res;
        }
        res.put("success", false);
        res.put("message", "用户名或密码错误");
        return res;
    }

    /** 仅接受测试用户 user1/user2/user3，拒绝管理员 */
    @PostMapping("/login/test")
    public Map<String, Object> testUserLogin(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        Map<String, Object> res = new HashMap<>();
        if (adminUsername.equals(username)) {
            res.put("success", false);
            res.put("message", "请使用管理员登录页登录");
            return res;
        }
        if (isTestUser(username) && TEST_USER_PASSWORD.equals(password)) {
            session.removeAttribute(SESSION_ADMIN);
            session.setAttribute(SESSION_USER, username);
            if (riskEscalationService != null) riskEscalationService.clearSessionState(session);
            res.put("success", true);
            res.put("message", "登录成功");
            return res;
        }
        res.put("success", false);
        res.put("message", "用户名或密码错误");
        return res;
    }

    private static boolean isTestUser(String username) {
        return "user1".equals(username) || "user2".equals(username) || "user3".equals(username);
    }
}
