package com.test.service;

import com.test.Entity.AuditLog;

import java.util.List;

/**
 * 审计日志服务（权限修改、注销DID 记录）
 */
public interface AuditLogService {

    /** 操作类型：权限修改 */
    String OPERATION_UPDATE_ROLE = "UPDATE_ROLE";
    /** 操作类型：注销DID */
    String OPERATION_REVOKE_DID = "REVOKE_DID";

    /**
     * 记录一条审计日志
     */
    void save(String operationType, Long targetIdentityId, String targetDid,
              String targetEmployeeId, String targetName, String detail);

    /**
     * 查询全部审计日志，按时间倒序
     */
    List<AuditLog> listAll();
}
