package com.test.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

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
}
