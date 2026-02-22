package com.test.blockchain;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 未启用区块链时的占位实现，不执行上链。
 */
@Service
@ConditionalOnMissingBean(ChainEvidenceServiceImpl.class)
public class ChainEvidenceServiceNoOp implements ChainEvidenceService {

    @Override
    public EvidenceResult saveEvidence(String key, String hash) {
        return null;
    }
}
