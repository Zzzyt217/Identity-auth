package com.test.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.test.Entity.Identity;
import com.test.blockchain.ChainEvidenceService;
import com.test.Mapper.IdentityMapper;
import com.test.Service.AuditLogService;
import com.test.Service.IdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 方案A：应用内生成 DID（did:blockdia:emp:{工号}），存库
 */
@Service
public class IdentityServiceImpl implements IdentityService {

    private static final String DID_PREFIX = "did:blockdia:emp:";

    @Resource
    private IdentityMapper identityMapper;
    @Resource
    private AuditLogService auditLogService;
    @Resource
    private ChainEvidenceService chainEvidenceService;

    /** 默认角色 */
    private static final String DEFAULT_ROLE = "员工";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Identity register(String employeeId, String name, String department, String position, String role) {
        Identity existing = getByEmployeeId(employeeId);
        if (existing != null) {
            throw new IllegalArgumentException("工号已存在，请勿重复注册");
        }
        String did = generateDid(employeeId);
        String roleVal = (role != null && !role.trim().isEmpty()) ? role.trim() : DEFAULT_ROLE;
        if (!"员工".equals(roleVal) && !"超级管理员".equals(roleVal)) {
            roleVal = DEFAULT_ROLE;
        }
        Identity identity = new Identity();
        identity.setEmployeeId(employeeId);
        identity.setName(name);
        identity.setDepartment(department);
        identity.setPosition(position);
        identity.setRole(roleVal);
        identity.setDid(did);
        identity.setCreatedAt(LocalDateTime.now());
        identityMapper.insert(identity);
        // 用 DB 中实际存储的那条记录算 hash 再上链，避免 created_at 与内存值不一致导致验证不通过
        Identity identityFromDb = identityMapper.selectById(identity.getId());
        String contentHash = identityFromDb != null ? Identity.computeContentHash(identityFromDb) : Identity.computeContentHash(identity);
        ChainEvidenceService.EvidenceResult evidence = chainEvidenceService.saveEvidence(identity.getDid(), contentHash);
        if (evidence != null) {
            identity.setChainTxHash(evidence.getTransactionHash());
            identity.setChainBlockNumber(evidence.getBlockNumber());
            identityMapper.updateById(identity);
        }
        return identity;
    }

    @Override
    public Identity getById(Long id) {
        return identityMapper.selectById(id);
    }

    @Override
    public Identity getByDid(String did) {
        return identityMapper.selectOne(new LambdaQueryWrapper<Identity>().eq(Identity::getDid, did));
    }

    @Override
    public Identity getByEmployeeId(String employeeId) {
        return identityMapper.selectOne(new LambdaQueryWrapper<Identity>().eq(Identity::getEmployeeId, employeeId));
    }

    @Override
    public Identity getByChainTxHash(String chainTxHash) {
        if (chainTxHash == null || chainTxHash.trim().isEmpty()) return null;
        return identityMapper.selectOne(
                new LambdaQueryWrapper<Identity>().eq(Identity::getChainTxHash, chainTxHash.trim()).last("LIMIT 1"));
    }

    @Override
    public List<Identity> listAll() {
        return identityMapper.selectList(null);
    }

    @Override
    public long count() {
        return identityMapper.selectCount(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, String role) {
        Identity identity = identityMapper.selectById(id);
        if (identity == null) {
            throw new IllegalArgumentException("身份不存在");
        }
        String oldRole = identity.getRole();
        String roleVal = (role != null && !role.trim().isEmpty()) ? role.trim() : DEFAULT_ROLE;
        if (!"员工".equals(roleVal) && !"超级管理员".equals(roleVal)) {
            roleVal = DEFAULT_ROLE;
        }
        identity.setRole(roleVal);
        identityMapper.updateById(identity);
        auditLogService.save(AuditLogService.OPERATION_UPDATE_ROLE, id, identity.getDid(),
                identity.getEmployeeId(), identity.getName(), oldRole + " -> " + roleVal);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        Identity identity = identityMapper.selectById(id);
        if (identity == null) {
            throw new IllegalArgumentException("身份不存在");
        }
        auditLogService.save(AuditLogService.OPERATION_REVOKE_DID, identity.getId(), identity.getDid(),
                identity.getEmployeeId(), identity.getName(), "注销DID");
        identityMapper.deleteById(id);
    }

    /**
     * 生成 DID：did:blockdia:emp:{工号}（工号唯一即可保证 DID 唯一）
     */
    private String generateDid(String employeeId) {
        return DID_PREFIX + employeeId.trim();
    }
}
