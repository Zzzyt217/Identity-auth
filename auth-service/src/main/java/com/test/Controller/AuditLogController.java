package com.test.Controller;

import com.test.Entity.AuditLog;
import com.test.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审计日志 API
 */
@RestController
@RequestMapping("/api")
public class AuditLogController {

    @Resource
    private AuditLogService auditLogService;

    /**
     * 审计日志列表，按时间倒序
     */
    @GetMapping("/audit/list")
    public Map<String, Object> list() {
        List<AuditLog> list = auditLogService.listAll();
        List<Map<String, Object>> items = list.stream().map(log -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", log.getId());
            m.put("operationType", log.getOperationType());
            m.put("targetIdentityId", log.getTargetIdentityId());
            m.put("targetDid", log.getTargetDid());
            m.put("targetEmployeeId", log.getTargetEmployeeId());
            m.put("targetName", log.getTargetName());
            m.put("detail", log.getDetail());
            m.put("operatedAt", log.getOperatedAt() != null ? log.getOperatedAt().toString() : null);
            m.put("chainTxHash", log.getChainTxHash());
            m.put("chainBlockNumber", log.getChainBlockNumber());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("list", items);
        return data;
    }
}
