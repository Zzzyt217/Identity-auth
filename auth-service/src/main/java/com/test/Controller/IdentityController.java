package com.test.Controller;

import com.test.Entity.Identity;
import com.test.Service.IdentityService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DID 身份 API（方案A：注册、查询、获取凭证）
 */
@RestController
@RequestMapping("/api")
public class IdentityController {

    @Resource
    private IdentityService identityService;

    /**
     * 注册：生成 DID 并入库，返回身份信息（含 id、did）
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        String name = body.get("name");
        String department = body.get("department");
        String position = body.get("position");
        String role = body.get("role");
        if (employeeId == null || employeeId.trim().isEmpty()
                || name == null || name.trim().isEmpty()
                || department == null || department.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "工号、姓名、部门为必填项");
            return err;
        }
        try {
            Identity identity = identityService.register(
                    employeeId.trim(), name.trim(), department.trim(),
                    position != null ? position.trim() : null,
                    role != null ? role.trim() : null);
            Map<String, Object> ok = new HashMap<>();
            ok.put("success", true);
            ok.put("message", "注册成功，DID 已生成");
            ok.put("id", identity.getId());
            ok.put("did", identity.getDid());
            ok.put("employeeId", identity.getEmployeeId());
            ok.put("name", identity.getName());
            ok.put("department", identity.getDepartment());
            ok.put("position", identity.getPosition());
            ok.put("role", identity.getRole());
            return ok;
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return err;
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "注册失败：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            return err;
        }
    }

    /**
     * 按主键获取身份（用于凭证页）
     */
    @GetMapping("/identity/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Identity identity = identityService.getById(id);
        if (identity == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "身份不存在");
            return err;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("id", identity.getId());
        data.put("did", identity.getDid());
        data.put("employeeId", identity.getEmployeeId());
        data.put("name", identity.getName());
        data.put("department", identity.getDepartment());
        data.put("position", identity.getPosition());
        data.put("role", identity.getRole());
        data.put("createdAt", identity.getCreatedAt() != null ? identity.getCreatedAt().toString() : null);
        return data;
    }

    /**
     * 查询：按 DID 或工号查询
     */
    @GetMapping("/identity/query")
    public Map<String, Object> query(@RequestParam(required = false) String did,
                                      @RequestParam(required = false) String employeeId) {
        if ((did == null || did.trim().isEmpty()) && (employeeId == null || employeeId.trim().isEmpty())) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "请填写 DID 或工号");
            return err;
        }
        Identity identity = null;
        if (did != null && !did.trim().isEmpty()) {
            identity = identityService.getByDid(did.trim());
        }
        if (identity == null && employeeId != null && !employeeId.trim().isEmpty()) {
            identity = identityService.getByEmployeeId(employeeId.trim());
        }
        if (identity == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "未查到对应身份");
            return err;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("id", identity.getId());
        data.put("did", identity.getDid());
        data.put("employeeId", identity.getEmployeeId());
        data.put("name", identity.getName());
        data.put("department", identity.getDepartment());
        data.put("position", identity.getPosition());
        data.put("role", identity.getRole());
        data.put("createdAt", identity.getCreatedAt() != null ? identity.getCreatedAt().toString() : null);
        data.put("chainTxHash", identity.getChainTxHash());
        data.put("chainBlockNumber", identity.getChainBlockNumber());
        return data;
    }

    /**
     * 权限管理：查询全部身份列表
     */
    @GetMapping("/identity/list")
    public Map<String, Object> list() {
        List<Identity> list = identityService.listAll();
        List<Map<String, Object>> items = list.stream().map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", i.getId());
            m.put("did", i.getDid());
            m.put("employeeId", i.getEmployeeId());
            m.put("name", i.getName());
            m.put("department", i.getDepartment());
            m.put("position", i.getPosition());
            m.put("role", i.getRole());
            m.put("createdAt", i.getCreatedAt() != null ? i.getCreatedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("list", items);
        return data;
    }

    /**
     * 权限管理：修改员工权限角色
     */
    @PutMapping("/identity/{id}/role")
    public Map<String, Object> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body != null ? body.get("role") : null;
        try {
            identityService.updateRole(id, role);
            Map<String, Object> ok = new HashMap<>();
            ok.put("success", true);
            ok.put("message", "权限已更新");
            return ok;
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return err;
        }
    }

    /**
     * 权限管理：注销 DID（删除该身份）
     */
    @DeleteMapping("/identity/{id}")
    public Map<String, Object> revoke(@PathVariable Long id) {
        try {
            identityService.revoke(id);
            Map<String, Object> ok = new HashMap<>();
            ok.put("success", true);
            ok.put("message", "DID 已注销");
            return ok;
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return err;
        }
    }
}
