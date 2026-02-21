package com.test.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员登录/登出（固定账号，Session 标记）
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    @Value("${admin.username:admin}")
    private String adminUsername;
    @Value("${admin.password:123456}")
    private String adminPassword;

    private static final String SESSION_ADMIN = "admin";

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        Map<String, Object> res = new HashMap<>();
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            session.setAttribute(SESSION_ADMIN, true);
            res.put("success", true);
            res.put("message", "登录成功");
            return res;
        }
        res.put("success", false);
        res.put("message", "用户名或密码错误");
        return res;
    }
}
