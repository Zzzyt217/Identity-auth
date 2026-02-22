package com.test.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.Entity.AuditLog;
import com.test.Mapper.AuditLogMapper;
import com.test.Service.AuditLogService;
import com.test.blockchain.ChainEvidenceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Resource
    private AuditLogMapper auditLogMapper;
    @Resource
    private ChainEvidenceService chainEvidenceService;

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
        // 用 DB 中实际存储的那条记录算 hash 再上链（与身份注册一致，避免时间精度导致验证不一致）
        AuditLog logFromDb = auditLogMapper.selectById(log.getId());
        String contentHash = logFromDb != null ? AuditLog.computeContentHash(logFromDb) : AuditLog.computeContentHash(log);
        ChainEvidenceService.EvidenceResult evidence = chainEvidenceService.saveEvidence("audit_" + log.getId(), contentHash);
        if (evidence != null) {
            log.setChainTxHash(evidence.getTransactionHash());
            log.setChainBlockNumber(evidence.getBlockNumber());
            auditLogMapper.updateById(log);
        }
    }

    @Override
    public List<AuditLog> listAll() {
        return auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getId));
    }

    @Override
    public AuditLog getByChainTxHash(String chainTxHash) {
        if (chainTxHash == null || chainTxHash.trim().isEmpty()) return null;
        return auditLogMapper.selectOne(
                new LambdaQueryWrapper<AuditLog>().eq(AuditLog::getChainTxHash, chainTxHash.trim()).last("LIMIT 1"));
    }
}
