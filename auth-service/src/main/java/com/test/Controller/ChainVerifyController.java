package com.test.Controller;

import com.test.Entity.AuditLog;
import com.test.Entity.Identity;
import com.test.Service.AuditLogService;
import com.test.Service.IdentityService;
import com.test.blockchain.EvidenceStorageContract;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.tx.gas.ContractGasProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * 链上验证 API：身份验证（DID + 预期/实际 hash 比对）、操作认证（交易存在性）等。
 */
@RestController
@RequestMapping("/api/chain")
public class ChainVerifyController {

    @Autowired
    private IdentityService identityService;
    @Autowired
    private AuditLogService auditLogService;

    @Autowired(required = false)
    private Web3j web3j;
    @Autowired(required = false)
    private Credentials credentials;
    @Autowired(required = false)
    private ContractGasProvider gasProvider;

    @Value("${fisco.evidence-contract-address:}")
    private String contractAddress;

    /** 获取当前链上区块高度，供首页展示 */
    @GetMapping("/block-number")
    public Map<String, Object> getBlockNumber() {
        Map<String, Object> result = new HashMap<>();
        if (web3j == null) {
            result.put("blockNumber", null);
            return result;
        }
        try {
            BigInteger bn = web3j.getBlockNumber().send().getBlockNumber();
            result.put("blockNumber", bn != null ? bn.longValue() : null);
        } catch (Exception e) {
            result.put("blockNumber", null);
        }
        return result;
    }

    /**
     * 身份验证：根据 DID 核验当前凭证内容与链上存证 hash 是否一致。
     * GET /api/chain/verify/identity?did=xxx&txHash=yyy（txHash 选填，仅用于前端展示或扩展）
     */
    @GetMapping("/verify/identity")
    public Map<String, Object> verifyIdentity(@RequestParam String did,
                                              @RequestParam(required = false) String txHash) {
        Map<String, Object> result = new HashMap<>();
        did = did != null ? did.trim() : "";
        if (did.isEmpty()) {
            result.put("status", "ERROR");
            result.put("message", "请提供 DID");
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
        Identity identity = identityService.getByDid(did);
        if (identity == null) {
            result.put("status", "NO_RECORD");
            result.put("message", "该身份不存在或链上无此 DID 记录");
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
        if (web3j == null || credentials == null || gasProvider == null
                || contractAddress == null || contractAddress.trim().isEmpty()) {
            result.put("status", "ERROR");
            result.put("message", "链上服务未配置或未就绪");
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
        try {
            String expectedHash = computeIdentityHash(identity);
            String actualHash = EvidenceStorageContract.get(
                    web3j, credentials, gasProvider, contractAddress.trim(), did);
            if (actualHash == null) {
                actualHash = "";
            }
            actualHash = actualHash.trim();
            if (actualHash.isEmpty()) {
                result.put("status", "NO_RECORD");
                result.put("message", "链上无存证：该身份尚未上链或链上无此 DID 记录。");
                result.put("expectedHash", expectedHash);
                result.put("actualHash", null);
                return result;
            }
            if (actualHash.equals(expectedHash)) {
                result.put("status", "PASS");
                result.put("message", "链上核验通过：当前凭证内容与链上存证一致，未被篡改。");
                result.put("expectedHash", expectedHash);
                result.put("actualHash", actualHash);
                return result;
            }
            result.put("status", "MISMATCH");
            result.put("message", "链上检验不通过：该身份存在问题（链上存证与当前内容不一致）。");
            result.put("expectedHash", expectedHash);
            result.put("actualHash", actualHash);
            return result;
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "链上查询失败：" + (e.getMessage() != null ? e.getMessage() : "请稍后重试"));
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
    }

    /**
     * 操作认证：根据交易哈希核验存证。支持身份注册交易与审计操作交易：先查审计记录，若无则查身份记录，核验预期/实际 hash。
     * GET /api/chain/verify/audit?txHash=xxx
     */
    @GetMapping("/verify/audit")
    public Map<String, Object> verifyAudit(@RequestParam String txHash) {
        Map<String, Object> result = new HashMap<>();
        txHash = txHash != null ? txHash.trim() : "";
        if (txHash.isEmpty()) {
            result.put("status", "ERROR");
            result.put("message", "请提供交易哈希");
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
        if (web3j == null || credentials == null || gasProvider == null
                || contractAddress == null || contractAddress.trim().isEmpty()) {
            result.put("status", "ERROR");
            result.put("message", "链上服务未配置或未就绪");
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
        // 1) 先按交易哈希查审计记录（权限修改、注销等）
        AuditLog log = auditLogService.getByChainTxHash(txHash);
        if (log != null) {
            return verifyAuditRecord(result, log);
        }
        // 2) 再按交易哈希查身份记录（注册上链）
        Identity identity = identityService.getByChainTxHash(txHash);
        if (identity != null) {
            return verifyIdentityRecord(result, identity);
        }
        result.put("status", "NO_RECORD");
        result.put("message", "未找到该交易对应的存证记录。请确认交易哈希是否正确，且为身份注册或权限/注销等已上链操作。");
        result.put("expectedHash", null);
        result.put("actualHash", null);
        return result;
    }

    private Map<String, Object> verifyAuditRecord(Map<String, Object> result, AuditLog log) {
        try {
            String key = "audit_" + log.getId();
            String expectedHash = AuditLog.computeContentHash(log);
            String actualHash = EvidenceStorageContract.get(
                    web3j, credentials, gasProvider, contractAddress.trim(), key);
            if (actualHash == null) actualHash = "";
            actualHash = actualHash.trim();
            if (actualHash.isEmpty()) {
                result.put("status", "NO_RECORD");
                result.put("message", "链上无该操作存证记录。");
                result.put("expectedHash", expectedHash);
                result.put("actualHash", null);
                return result;
            }
            if (actualHash.equals(expectedHash)) {
                result.put("status", "PASS");
                result.put("message", "链上核验通过（操作存证）：该笔操作凭证内容与链上存证一致，未被篡改。");
                result.put("expectedHash", expectedHash);
                result.put("actualHash", actualHash);
                return result;
            }
            result.put("status", "MISMATCH");
            result.put("message", "链上检验不通过：该操作凭证与链上存证内容不一致。");
            result.put("expectedHash", expectedHash);
            result.put("actualHash", actualHash);
            return result;
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "链上查询失败：" + (e.getMessage() != null ? e.getMessage() : "请稍后重试"));
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
    }

    private Map<String, Object> verifyIdentityRecord(Map<String, Object> result, Identity identity) {
        try {
            String expectedHash = Identity.computeContentHash(identity);
            String actualHash = EvidenceStorageContract.get(
                    web3j, credentials, gasProvider, contractAddress.trim(), identity.getDid());
            if (actualHash == null) actualHash = "";
            actualHash = actualHash.trim();
            if (actualHash.isEmpty()) {
                result.put("status", "NO_RECORD");
                result.put("message", "链上无该身份存证记录。");
                result.put("expectedHash", expectedHash);
                result.put("actualHash", null);
                return result;
            }
            if (actualHash.equals(expectedHash)) {
                result.put("status", "PASS");
                result.put("message", "链上核验通过（身份注册存证）：当前凭证内容与链上存证一致，未被篡改。");
                result.put("expectedHash", expectedHash);
                result.put("actualHash", actualHash);
                return result;
            }
            result.put("status", "MISMATCH");
            result.put("message", "链上检验不通过：该身份凭证与链上存证内容不一致。");
            result.put("expectedHash", expectedHash);
            result.put("actualHash", actualHash);
            return result;
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "链上查询失败：" + (e.getMessage() != null ? e.getMessage() : "请稍后重试"));
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
    }

    /**
     * 与 IdentityServiceImpl 注册上链时保持完全一致的 hash 计算规则（共用 Identity.buildContentString）。
     */
    private String computeIdentityHash(Identity identity) {
        return Identity.computeContentHash(identity);
    }
}
