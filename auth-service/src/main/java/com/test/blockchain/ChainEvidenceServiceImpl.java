package com.test.blockchain;

import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.tx.gas.ContractGasProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 链上存证实现：当 fisco.enabled=true 且配置了合约地址时，调用 EvidenceStorage.set 并返回 txHash、blockNumber。
 */
@Service
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true")
public class ChainEvidenceServiceImpl implements ChainEvidenceService {

    private static final Logger log = LoggerFactory.getLogger(ChainEvidenceServiceImpl.class);

    @Autowired(required = false)
    private Web3j web3j;
    @Autowired(required = false)
    private Credentials credentials;
    @Autowired(required = false)
    private ContractGasProvider gasProvider;

    @Value("${fisco.evidence-contract-address:}")
    private String contractAddress;

    @Override
    public ChainEvidenceService.EvidenceResult saveEvidence(String key, String hash) {
        if (web3j == null || credentials == null || gasProvider == null
                || contractAddress == null || contractAddress.trim().isEmpty()) {
            log.debug("[区块链] 未配置合约地址或 SDK 未就绪，跳过上链 key={}", key);
            return null;
        }
        try {
            TransactionReceipt receipt = EvidenceStorageContract.set(
                    web3j, credentials, gasProvider, contractAddress.trim(), key, hash);
            if (receipt == null || !receipt.isStatusOK()) {
                log.warn("[区块链] 上链失败 key={}, status={}", key, receipt != null ? receipt.getStatus() : "null");
                return null;
            }
            String txHash = receipt.getTransactionHash();
            Long blockNumber = receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null;
            log.info("[区块链] 存证成功 key={}, txHash={}, blockNumber={}", key, txHash, blockNumber);
            return new ChainEvidenceService.EvidenceResult(txHash, blockNumber);
        } catch (Exception e) {
            log.warn("[区块链] 上链异常 key={}, error={}", key, e.getMessage());
            return null;
        }
    }
}
