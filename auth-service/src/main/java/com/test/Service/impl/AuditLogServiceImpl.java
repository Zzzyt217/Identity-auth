package com.test.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.Entity.AuditLog;
import com.test.mapper.AuditLogMapper;
import com.test.service.AuditLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Resource
    private AuditLogMapper auditLogMapper;

    @Override
    public void save(String operationType, Long targetIdentityId, String targetDid,
                     String targetEmployeeId, String targetName, String detail) {
        AuditLog log = new AuditLog();
        log.setOperationType(operationType);
        log.setTargetIdentityId(targetIdentityId);
        log.setTargetDid(targetDid);
        log.setTargetEmployeeId(targetEmployeeId);
        log.setTargetName(targetName);
        log.setDetail(detail);
        log.setOperatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    @Override
    public List<AuditLog> listAll() {
        return auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getId));
    }
}
