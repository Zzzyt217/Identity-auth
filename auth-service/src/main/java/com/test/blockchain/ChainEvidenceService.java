package com.test.blockchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 链上存证服务：将 key-hash 写入步骤 2 部署的 EvidenceStorage 合约。
 * 未启用或未配置合约地址时不执行上链，返回 null。
 */
public interface ChainEvidenceService {

    /**
     * 将一条存证写入链上。
     *
     * @param key  存证键（如 DID 或 "audit_日志id"）
     * @param hash 存证内容 hash（如身份摘要或审计摘要的 SHA256）
     * @return 成功时返回交易哈希与区块号，未上链或失败时返回 null
     */
    EvidenceResult saveEvidence(String key, String hash);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class EvidenceResult {
        private String transactionHash;
        private Long blockNumber;
    }
}
