package com.test.Controller;

import com.test.Entity.AuditLog;
import com.test.Entity.Identity;
import com.test.Service.AuditLogService;
import com.test.Service.IdentityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 存证记录汇总页：仅展示已上链的身份存证与审计存证，不修改任何现有功能。
 */
@Controller
@RequestMapping("/blockchain")
public class EvidenceRecordController {

    @Resource
    private IdentityService identityService;
    @Resource
    private AuditLogService auditLogService;

    /** 存证记录入口：选择身份存证或审计存证 */
    @GetMapping("/evidence")
    public String evidenceChoosePage() {
        return "evidence-choose";
    }

    private static final int PAGE_SIZE = 6;

    /** 身份存证列表（每页 6 条） */
    @GetMapping("/evidence/identity")
    public String evidenceIdentityPage(Model model, @RequestParam(defaultValue = "1") int page) {
        List<Identity> fullList = identityService.listAll().stream()
                .filter(i -> i.getChainTxHash() != null && !i.getChainTxHash().isEmpty())
                .collect(Collectors.toList());
        int total = fullList.size();
        int totalPages = total == 0 ? 1 : (total + PAGE_SIZE - 1) / PAGE_SIZE;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        List<Identity> identityRecords = from < total ? fullList.subList(from, to) : new ArrayList<>();
        model.addAttribute("identityRecords", identityRecords);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "evidence-identity";
    }

    /** 审计存证列表：包含身份注册与审计操作（权限修改、注销DID）的已上链记录（每页 6 条） */
    @GetMapping("/evidence/audit")
    public String evidenceAuditPage(Model model, @RequestParam(defaultValue = "1") int page) {
        List<Map<String, Object>> allRecords = new ArrayList<>();
        // 身份注册记录（带交易 hash 的视为已上链存证）
        List<Identity> identityRecords = identityService.listAll().stream()
                .filter(i -> i.getChainTxHash() != null && !i.getChainTxHash().isEmpty())
                .collect(Collectors.toList());
        for (Identity i : identityRecords) {
            Map<String, Object> m = new HashMap<>();
            m.put("operationTypeDisplay", "身份注册");
            m.put("targetDid", i.getDid());
            m.put("employeeId", i.getEmployeeId());
            m.put("name", i.getName());
            m.put("detail", "—");
            m.put("time", i.getCreatedAt());
            m.put("chainTxHash", i.getChainTxHash());
            m.put("chainBlockNumber", i.getChainBlockNumber());
            m.put("identityId", i.getId());
            allRecords.add(m);
        }
        // 审计操作记录
        List<AuditLog> auditRecords = auditLogService.listAll().stream()
                .filter(l -> l.getChainTxHash() != null && !l.getChainTxHash().isEmpty())
                .collect(Collectors.toList());
        for (AuditLog r : auditRecords) {
            Map<String, Object> m = new HashMap<>();
            m.put("operationTypeDisplay", AuditLogService.OPERATION_UPDATE_ROLE.equals(r.getOperationType()) ? "权限修改" : "注销DID");
            m.put("targetDid", r.getTargetDid());
            m.put("employeeId", r.getTargetEmployeeId());
            m.put("name", r.getTargetName());
            m.put("detail", r.getDetail());
            m.put("time", r.getOperatedAt());
            m.put("chainTxHash", r.getChainTxHash());
            m.put("chainBlockNumber", r.getChainBlockNumber());
            m.put("identityId", null);
            allRecords.add(m);
        }
        // 按时间倒序（时间晚的在前）
        allRecords.sort(Comparator.comparing(
                (Map<String, Object> row) -> (LocalDateTime) row.get("time"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        int total = allRecords.size();
        int totalPages = total == 0 ? 1 : (total + PAGE_SIZE - 1) / PAGE_SIZE;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);
        List<Map<String, Object>> pagedRecords = from < total ? new ArrayList<>(allRecords.subList(from, to)) : new ArrayList<>();
        model.addAttribute("allRecords", pagedRecords);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "evidence-audit";
    }

    // ---------- 链上验证 ----------

    /** 链上验证入口：选择身份验证或操作认证 */
    @GetMapping("/verify")
    public String verifyChoosePage() {
        return "verify-choose";
    }

    /** 身份验证页：输入 DID、交易 hash，核验凭证与链上存证是否一致 */
    @GetMapping("/verify/identity")
    public String verifyIdentityPage() {
        return "verify-identity";
    }

    /** 操作认证页：输入交易 hash，核验该笔交易是否上链 */
    @GetMapping("/verify/audit")
    public String verifyAuditPage() {
        return "verify-audit";
    }
}
