package com.test.Entity;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 审计日志实体（权限修改、注销DID；下一阶段可填充链上交易哈希）
 */
@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 操作类型：UPDATE_ROLE、REVOKE_DID */
    private String operationType;
    private Long targetIdentityId;
    private String targetDid;
    private String targetEmployeeId;
    private String targetName;
    /** 操作说明 */
    private String detail;
    private LocalDateTime operatedAt;
    private String chainTxHash;
    private Long chainBlockNumber;

    /** 存证/验证时 operatedAt 的格式（只到秒），与 Identity 一致 */
    public static final DateTimeFormatter HASH_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 用于链上存证与验证的拼接字符串，与 AuditLogServiceImpl 上链时一致。
     */
    public static String buildContentString(AuditLog log) {
        if (log == null) return "";
        String operatedAtStr = log.getOperatedAt() != null ? log.getOperatedAt().format(HASH_TIMESTAMP_FORMAT) : "";
        return (log.getOperationType() != null ? log.getOperationType() : "") + "|"
                + (log.getTargetDid() != null ? log.getTargetDid() : "") + "|"
                + (log.getTargetName() != null ? log.getTargetName() : "") + "|"
                + (log.getDetail() != null ? log.getDetail() : "") + "|"
                + operatedAtStr;
    }

    /** 与 buildContentString 配套：UTF-8 SHA256 hex，注册与验证共用。 */
    public static String computeContentHash(AuditLog log) {
        if (log == null) return "";
        byte[] bytes = buildContentString(log).getBytes(StandardCharsets.UTF_8);
        return DigestUtil.sha256Hex(bytes);
    }
}
