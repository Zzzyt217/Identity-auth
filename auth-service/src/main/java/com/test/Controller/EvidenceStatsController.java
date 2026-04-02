package com.test.Controller;

import com.test.blockchain.EvidenceStorageContract;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.tx.gas.ContractGasProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 链上存证统计接口：验证新合约功能
 */
@RestController
@RequestMapping("/api/evidence")
public class EvidenceStatsController {

    @Value("${fisco.evidence-contract-address:}")
    private String contractAddress;

    @Resource
    private Web3j web3j;

    @Resource
    private Credentials credentials;

    @Resource
    private ContractGasProvider gasProvider;

    /**
     * 获取链上存证统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            java.math.BigInteger totalRecords = EvidenceStorageContract.getTotalRecords(
                web3j, credentials, gasProvider, contractAddress);
            result.put("success", true);
            result.put("contractAddress", contractAddress);
            result.put("totalRecords", totalRecords);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 验证指定 DID 的存证是否存在
     */
    @GetMapping("/exists")
    public Map<String, Object> checkExists(@RequestParam String did) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean exists = EvidenceStorageContract.exists(
                web3j, credentials, gasProvider, contractAddress, did);
            result.put("success", true);
            result.put("did", did);
            result.put("exists", exists);
            result.put("message", exists ? "存证存在" : "存证不存在");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取指定 DID 的存证时间戳
     */
    @GetMapping("/timestamp")
    public Map<String, Object> getTimestamp(@RequestParam String did) {
        Map<String, Object> result = new HashMap<>();
        try {
            java.math.BigInteger timestamp = EvidenceStorageContract.getTimestamp(
                web3j, credentials, gasProvider, contractAddress, did);
            result.put("success", true);
            result.put("did", did);
            result.put("timestamp", timestamp);
            result.put("message", timestamp.longValue() > 0 ? 
                "上链时间: " + java.time.Instant.ofEpochSecond(timestamp.longValue()) : "存证不存在");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }
}
