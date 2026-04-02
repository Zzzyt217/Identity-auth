package com.test.Controller;

import com.test.Service.IdentityService;
import com.test.blockchain.EvidenceStorageContract;
import com.test.Entity.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.tx.gas.ContractGasProvider;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据迁移控制器：将旧合约的数据迁移到新合约
 */
@RestController
@RequestMapping("/api/migrate")
public class MigrationController {

    @Value("${fisco.evidence-contract-address:}")
    private String currentContractAddress;  // 当前使用的新合约地址

    @Value("${fisco.old-evidence-contract-address:}")
    private String oldContractAddress;  // 旧合约地址（仅迁移时使用）（待配置）

    @Autowired
    private IdentityService identityService;

    @Autowired
    private Web3j web3j;

    @Autowired
    private Credentials credentials;

    @Autowired
    private ContractGasProvider gasProvider;

    /**
     * 迁移所有身份数据到新合约
     */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> migrateToNewContract() {
        Map<String, Object> result = new HashMap<>();
        
        if (currentContractAddress == null || currentContractAddress.isEmpty()) {
            result.put("success", false);
            result.put("message", "请先配置新合约地址: fisco.evidence-contract-address");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            // 从数据库获取所有身份
            List<Identity> allIdentities = identityService.listAll();
            int successCount = 0;

            for (Identity identity : allIdentities) {
                String did = identity.getDid();
                String hash = Identity.computeContentHash(identity);
                
                // 检查新合约是否已有该记录
                boolean alreadyExists = EvidenceStorageContract.exists(
                    web3j, credentials, gasProvider, currentContractAddress, did);
                
                if (!alreadyExists) {
                    // 写入新合约
                    EvidenceStorageContract.set(
                        web3j, credentials, gasProvider, currentContractAddress, did, hash);
                    successCount++;
                }
            }

            result.put("success", true);
            result.put("message", "迁移完成");
            result.put("total", allIdentities.size());
            result.put("migrated", successCount);
            result.put("skipped", allIdentities.size() - successCount);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "迁移失败: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 预览迁移数据（不实际迁移）
     */
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewMigration() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Identity> allIdentities = identityService.listAll();
            int chainCount = EvidenceStorageContract.getTotalRecords(
                web3j, credentials, gasProvider, currentContractAddress).intValue();
            
            result.put("success", true);
            result.put("databaseCount", allIdentities.size());
            result.put("chainCount", chainCount);
            result.put("toMigrate", allIdentities.size() - chainCount);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}
