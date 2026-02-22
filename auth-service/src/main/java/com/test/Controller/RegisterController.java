package com.test.Controller;

import com.test.Entity.Identity;
import com.test.Service.IdentityService;
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

    /** 首页：根据是否管理员登录决定是否展示权限管理中心，并传入注册人数 */
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        model.addAttribute("isAdmin", session.getAttribute(SESSION_ADMIN) != null);
        model.addAttribute("didCount", identityService.count());
        return "index";
    }

    /** 管理员登录页 */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirect", required = false) String redirect, Model model) {
        model.addAttribute("redirect", redirect != null ? redirect : "/");
        return "login";
    }

    /** 登出：清除 Session 并跳转首页 */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_ADMIN);
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
}