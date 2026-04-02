package com.test.Controller;

import com.test.Entity.AuditLog;
import com.test.Entity.Identity;
import com.test.Service.AuditLogService;
import com.test.Service.IdentityService;
import com.test.Service.DashboardStatsService;
import com.test.blockchain.EvidenceStorageContract;
import com.test.risk.BehaviorRecordService;
import com.test.risk.AiRiskEngine;
import com.test.risk.RiskResult;
import com.test.risk.RiskEscalationService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.tx.gas.ContractGasProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private BehaviorRecordService behaviorRecordService;

    @Autowired
    private AiRiskEngine aiRiskEngine;
    @Autowired
    private RiskEscalationService riskEscalationService;
    @Autowired(required = false)
    private DashboardStatsService dashboardStatsService;

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
     * 支持两种定位方式：
     *  - 按 DID：GET /api/chain/verify/identity?did=xxx
     *  - 按姓名：GET /api/chain/verify/identity?name=xxx（若同名存在多个候选将返回 candidates 供二次选择）
     * txHash 选填：GET /api/chain/verify/identity?did=xxx&txHash=yyy（txHash 目前仅用于兼容/扩展）
     */
    @GetMapping("/verify/identity")
    public Map<String, Object> verifyIdentity(@RequestParam(required = false) String did,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) String txHash,
                                              HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        did = did != null ? did.trim() : "";
        name = name != null ? name.trim() : "";
        String userId = session != null && session.getAttribute("user") != null
                ? (String) session.getAttribute("user") : "anonymous";
        if (riskEscalationService != null && riskEscalationService.isBlocked(session)) {
            if (dashboardStatsService != null) dashboardStatsService.incrementHighRiskCount();
            return riskEscalationService.createRefuseResponse();
        }
        behaviorRecordService.record(userId, BehaviorRecordService.ACTION_CHAIN_VERIFY, System.currentTimeMillis());
        if (dashboardStatsService != null) dashboardStatsService.incrementVerifyCount();
        if (did.isEmpty() && name.isEmpty()) {
            attachRisk(result, userId, session);
            result.put("status", "ERROR");
            result.put("message", "请提供 DID 或姓名");
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }

        // 1) 先根据定位条件拿到唯一身份（或返回候选列表）
        Identity identity = null;
        String identityDid = null;
        if (!did.isEmpty()) {
            identity = identityService.getByDid(did);
            if (identity == null) {
                attachRisk(result, userId, session);
                result.put("status", "NO_RECORD");
                result.put("message", "该身份不存在或链上无此 DID 记录");
                result.put("expectedHash", null);
                result.put("actualHash", null);
                return result;
            }
            identityDid = identity.getDid();
        } else {
            List<Identity> all = identityService.listAll();
            List<Identity> candidates = new ArrayList<>();
            for (Identity i : all) {
                if (i == null || i.getName() == null) continue;
                if (i.getName().trim().equalsIgnoreCase(name)) {
                    candidates.add(i);
                }
            }
            if (candidates.isEmpty()) {
                attachRisk(result, userId, session);
                result.put("status", "NO_RECORD");
                result.put("message", "未查到该姓名对应的身份记录");
                result.put("expectedHash", null);
                result.put("actualHash", null);
                return result;
            }
            if (candidates.size() > 1) {
                // 同名存在多个候选：返回 candidates 给前端二次选择
                attachRisk(result, userId, session);
                result.put("status", "MULTIPLE_MATCHES");
                result.put("message", "检测到多个同名身份，请选择对应 DID 进行验证");
                List<Map<String, Object>> candidateList = new ArrayList<>();
                for (Identity c : candidates) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("did", c.getDid());
                    m.put("employeeId", c.getEmployeeId());
                    m.put("name", c.getName());
                    m.put("department", c.getDepartment());
                    m.put("position", c.getPosition());
                    m.put("role", c.getRole());
                    candidateList.add(m);
                }
                result.put("candidates", candidateList);
                result.put("expectedHash", null);
                result.put("actualHash", null);
                return result;
            }
            identity = candidates.get(0);
            identityDid = identity.getDid();
        }

        // 2) 单一身份：继续链上验证流程（链服务需就绪）
        if (web3j == null || credentials == null || gasProvider == null
                || contractAddress == null || contractAddress.trim().isEmpty()) {
            attachRisk(result, userId, session);
            result.put("status", "ERROR");
            result.put("message", "链上服务未配置或未就绪");
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }

        try {
            String expectedHash = computeIdentityHash(identity);
            String actualHash = EvidenceStorageContract.get(
                    web3j, credentials, gasProvider, contractAddress.trim(), identityDid);
            if (actualHash == null) {
                actualHash = "";
            }
            actualHash = actualHash.trim();
            if (actualHash.isEmpty()) {
                attachRisk(result, userId, session);
                result.put("status", "NO_RECORD");
                result.put("message", "链上无存证：该身份尚未上链或链上无此 DID 记录。");
                result.put("expectedHash", expectedHash);
                result.put("actualHash", null);
                return result;
            }
            if (actualHash.equals(expectedHash)) {
                attachRisk(result, userId, session);
                result.put("status", "PASS");
                result.put("message", "链上核验通过：当前凭证内容与链上存证一致，未被篡改。");
                result.put("expectedHash", expectedHash);
                result.put("actualHash", actualHash);
                return result;
            }
            attachRisk(result, userId, session);
            result.put("status", "MISMATCH");
            result.put("message", "链上检验不通过：该身份存在问题（链上存证与当前内容不一致）。");
            result.put("expectedHash", expectedHash);
            result.put("actualHash", actualHash);
            return result;
        } catch (Exception e) {
            attachRisk(result, userId, session);
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
    public Map<String, Object> verifyAudit(@RequestParam String txHash, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        txHash = txHash != null ? txHash.trim() : "";
        String userId = session != null && session.getAttribute("user") != null
                ? (String) session.getAttribute("user") : "anonymous";
        if (riskEscalationService != null && riskEscalationService.isBlocked(session)) {
            if (dashboardStatsService != null) dashboardStatsService.incrementHighRiskCount();
            return riskEscalationService.createRefuseResponse();
        }
        behaviorRecordService.record(userId, BehaviorRecordService.ACTION_CHAIN_VERIFY, System.currentTimeMillis());
        if (dashboardStatsService != null) dashboardStatsService.incrementVerifyCount();
        if (txHash.isEmpty()) {
            attachRisk(result, userId, session);
            result.put("status", "ERROR");
            result.put("message", "请提供交易哈希");
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
        if (web3j == null || credentials == null || gasProvider == null
                || contractAddress == null || contractAddress.trim().isEmpty()) {
            attachRisk(result, userId, session);
            result.put("status", "ERROR");
            result.put("message", "链上服务未配置或未就绪");
            result.put("expectedHash", null);
            result.put("actualHash", null);
            return result;
        }
        // 1) 先按交易哈希查审计记录（权限修改、注销等）
        AuditLog log = auditLogService.getByChainTxHash(txHash);
        if (log != null) {
            Map<String, Object> r = verifyAuditRecord(result, log);
            attachRisk(r, userId, session);
            return r;
        }
        // 2) 再按交易哈希查身份记录（注册上链）
        Identity identity = identityService.getByChainTxHash(txHash);
        if (identity != null) {
            Map<String, Object> r = verifyIdentityRecord(result, identity);
            attachRisk(r, userId, session);
            return r;
        }
        attachRisk(result, userId, session);
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

    /** 在响应中附带当前用户的风险检查结果，并做中风险预警后操作次数累计（供升级为拒绝）；中高风险时计入仪表盘统计。 */
    private void attachRisk(Map<String, Object> result, String userId, HttpSession session) {
        if (aiRiskEngine == null) return;
        try {
            RiskResult r = aiRiskEngine.evaluateRisk(userId);
            Map<String, Object> risk = new HashMap<>();
            risk.put("riskDetected", r.isRiskDetected());
            risk.put("riskLevel", r.getRiskLevel());
            risk.put("score", r.getScore());
            risk.put("message", r.getMessage());
            risk.put("riskSource", r.getRiskSource());
            if (r.getPosteriorProbability() != null) {
                risk.put("posteriorProbability", r.getPosteriorProbability());
            }
            result.put("risk", risk);
            if (riskEscalationService != null && session != null) {
                riskEscalationService.applyAfterRequest(session, r);
            }
            if (dashboardStatsService != null && ("高".equals(r.getRiskLevel()) || "中".equals(r.getRiskLevel()))) {
                dashboardStatsService.incrementHighRiskCount();
            }
        } catch (Exception ignored) { }
    }

    /**
     * 导出验证报告（Excel）：根据 txHash 导出单笔验证报告
     * GET /api/chain/report/export?txHash=xxx
     */
    @GetMapping("/report/export")
    public void exportReport(@RequestParam(required = false) String txHash,
                             @RequestParam(required = false) String did,
                             HttpServletResponse response) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XSSFWorkbook wb = new XSSFWorkbook();

        // ---------- 通用样式 ----------
        CellStyle titleStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle leftStyle = wb.createCellStyle();
        leftStyle.setAlignment(HorizontalAlignment.LEFT);
        leftStyle.setWrapText(true);

        CellStyle boldLeftStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font boldFont = wb.createFont();
        boldFont.setBold(true);
        boldLeftStyle.setFont(boldFont);
        boldLeftStyle.setAlignment(HorizontalAlignment.LEFT);

        CellStyle monoStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font monoFont = wb.createFont();
        monoFont.setFontName("Courier New");
        monoFont.setFontHeightInPoints((short) 10);
        monoStyle.setFont(monoFont);
        monoStyle.setAlignment(HorizontalAlignment.LEFT);

        // ---------- Sheet1：验证汇总 ----------
        Sheet sheet1 = wb.createSheet("验证汇总");
        sheet1.setColumnWidth(0, 6000);
        sheet1.setColumnWidth(1, 12000);

        int s1Row = 0;
        Row metaTitle = sheet1.createRow(s1Row++);
        Cell metaTitleCell = metaTitle.createCell(0);
        metaTitleCell.setCellValue("数字身份链上验证报告");
        metaTitleCell.setCellStyle(titleStyle);
        sheet1.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

        s1Row++; // 空行

        // 基本信息区块
        Row sec1Header = sheet1.createRow(s1Row++);
        Cell sec1Cell = sec1Header.createCell(0);
        sec1Cell.setCellValue("一、验证基本信息");
        sec1Cell.setCellStyle(boldLeftStyle);
        sheet1.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(s1Row - 1, s1Row - 1, 0, 1));

        String verifyType = "—";
        String verifyTarget = "";
        String verifyStatus = "—";
        String chainAvailable = "不可用";
        Identity identity = null;
        AuditLog auditLog = null;
        String expectedHash = "";
        String actualHash = "";
        String txHashValue = "";
        String remark = "";

        if (txHash != null && !txHash.trim().isEmpty()) {
            verifyType = "操作认证（按交易哈希）";
            txHashValue = txHash.trim();
            auditLog = auditLogService.getByChainTxHash(txHashValue);
            if (auditLog != null) {
                identity = identityService.getById(auditLog.getTargetIdentityId());
                expectedHash = AuditLog.computeContentHash(auditLog);
                actualHash = getChainHashByKey("audit_" + auditLog.getId());
                verifyTarget = identity != null ? identity.getDid() : "";
                verifyStatus = getVerifyStatusText(expectedHash, actualHash);
                remark = "操作认证：权限修改/注销";
            } else {
                identity = identityService.getByChainTxHash(txHashValue);
                if (identity != null) {
                    expectedHash = Identity.computeContentHash(identity);
                    actualHash = getChainHashByKey(identity.getDid());
                    verifyTarget = identity.getDid();
                    verifyStatus = getVerifyStatusText(expectedHash, actualHash);
                    remark = "操作认证：身份注册上链";
                }
            }
        } else if (did != null && !did.trim().isEmpty()) {
            verifyType = "身份验证（按DID）";
            verifyTarget = did.trim();
            identity = identityService.getByDid(verifyTarget);
            if (identity != null) {
                expectedHash = Identity.computeContentHash(identity);
                actualHash = getChainHashByKey(identity.getDid());
                txHashValue = identity.getChainTxHash() != null ? identity.getChainTxHash() : "";
                verifyStatus = getVerifyStatusText(expectedHash, actualHash);
                remark = "身份验证：DID存证核验";
            }
        }

        if (web3j != null && credentials != null && gasProvider != null
                && contractAddress != null && !contractAddress.trim().isEmpty()) {
            chainAvailable = "可用";
        }

        // 基本信息行
        addSummaryRow(sheet1, s1Row++, "报告生成时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        addSummaryRow(sheet1, s1Row++, "验证类型", verifyType);
        addSummaryRow(sheet1, s1Row++, "验证对象DID", verifyTarget);
        addSummaryRow(sheet1, s1Row++, "验证结果", verifyStatus);
        addSummaryRow(sheet1, s1Row++, "链上服务状态", chainAvailable);

        s1Row++;

        // Hash 详情区块
        Row sec2Header = sheet1.createRow(s1Row++);
        Cell sec2Cell = sec2Header.createCell(0);
        sec2Cell.setCellValue("二、Hash 核对结果");
        sec2Cell.setCellStyle(boldLeftStyle);
        sheet1.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(s1Row - 1, s1Row - 1, 0, 1));

        addSummaryRow(sheet1, s1Row++, "交易哈希（链上）", txHashValue);
        addHashRow(sheet1, s1Row++, "预期Hash（数据库）", expectedHash);
        addHashRow(sheet1, s1Row++, "实际Hash（链上存证）", actualHash);
        addSummaryRow(sheet1, s1Row++, "Hash比对", verifyStatus);

        s1Row++;

        // 身份信息区块（如果有）
        if (identity != null) {
            Row sec3Header = sheet1.createRow(s1Row++);
            Cell sec3Cell = sec3Header.createCell(0);
            sec3Cell.setCellValue("三、身份信息摘要");
            sec3Cell.setCellStyle(boldLeftStyle);
            sheet1.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(s1Row - 1, s1Row - 1, 0, 1));

            addSummaryRow(sheet1, s1Row++, "DID", identity.getDid() != null ? identity.getDid() : "—");
            addSummaryRow(sheet1, s1Row++, "员工号", identity.getEmployeeId() != null ? identity.getEmployeeId() : "—");
            addSummaryRow(sheet1, s1Row++, "姓名", identity.getName() != null ? identity.getName() : "—");
            addSummaryRow(sheet1, s1Row++, "部门", identity.getDepartment() != null ? identity.getDepartment() : "—");
            addSummaryRow(sheet1, s1Row++, "职位", identity.getPosition() != null ? identity.getPosition() : "—");
            addSummaryRow(sheet1, s1Row++, "角色", identity.getRole() != null ? identity.getRole() : "—");
            addSummaryRow(sheet1, s1Row++, "身份状态", "已激活");
            addSummaryRow(sheet1, s1Row++, "链上注册时间", identity.getCreatedAt() != null
                    ? identity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "—");
            addSummaryRow(sheet1, s1Row++, "验证说明", remark);
        }

        // ---------- Sheet2：审计操作记录 ----------
        Sheet sheet2 = wb.createSheet("操作记录");
        sheet2.setColumnWidth(0, 5000);
        sheet2.setColumnWidth(1, 8000);
        sheet2.setColumnWidth(2, 12000);

        Row auditHeader = sheet2.createRow(0);
        auditHeader.createCell(0).setCellValue("项目");
        auditHeader.createCell(1).setCellValue("内容");
        auditHeader.getCell(0).setCellStyle(boldLeftStyle);
        auditHeader.getCell(1).setCellStyle(boldLeftStyle);

        int auditRow = 1;
        if (auditLog != null) {
            addAuditRow(sheet2, auditRow++, "操作类型", auditLog.getOperationType() != null ? auditLog.getOperationType() : "—");
            addAuditRow(sheet2, auditRow++, "目标DID", auditLog.getTargetDid() != null ? auditLog.getTargetDid() : "—");
            addAuditRow(sheet2, auditRow++, "目标姓名", auditLog.getTargetName() != null ? auditLog.getTargetName() : "—");
            addAuditRow(sheet2, auditRow++, "链上交易哈希", auditLog.getChainTxHash() != null ? auditLog.getChainTxHash() : "—");
            addAuditRow(sheet2, auditRow++, "链上存证Key", "audit_" + auditLog.getId());
            addAuditRow(sheet2, auditRow++, "操作时间", auditLog.getOperatedAt() != null
                    ? auditLog.getOperatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "—");
            addAuditRow(sheet2, auditRow++, "操作说明", auditLog.getDetail() != null ? auditLog.getDetail() : "—");
        } else if (identity != null) {
            addAuditRow(sheet2, auditRow++, "操作类型", "身份注册上链");
            addAuditRow(sheet2, auditRow++, "DID", identity.getDid() != null ? identity.getDid() : "—");
            addAuditRow(sheet2, auditRow++, "链上交易哈希", identity.getChainTxHash() != null ? identity.getChainTxHash() : "—");
            addAuditRow(sheet2, auditRow++, "链上存证Key", identity.getDid() != null ? identity.getDid() : "—");
            addAuditRow(sheet2, auditRow++, "注册时间", identity.getCreatedAt() != null
                    ? identity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "—");
            addAuditRow(sheet2, auditRow++, "操作描述", "身份注册并上链存证");
        }

        // ---------- Sheet3：区块链存证信息 ----------
        Sheet sheet3 = wb.createSheet("区块链存证");
        sheet3.setColumnWidth(0, 5000);
        sheet3.setColumnWidth(1, 12000);

        Row chainTitle = sheet3.createRow(0);
        Cell chainTitleCell = chainTitle.createCell(0);
        chainTitleCell.setCellValue("区块链存证验证信息");
        chainTitleCell.setCellStyle(boldLeftStyle);
        sheet3.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

        int cRow = 1;
        addChainRow(sheet3, cRow++, "区块链平台", "FISCO BCOS 2.x");
        addChainRow(sheet3, cRow++, "链版本", "Ethereum compatible");
        addChainRow(sheet3, cRow++, "存证合约地址", contractAddress != null && !contractAddress.trim().isEmpty() ? contractAddress.trim() : "未配置");
        addChainRow(sheet3, cRow++, "当前区块高度", getCurrentBlockNumber());
        addChainRow(sheet3, cRow++, "合约名称", "EvidenceStorage");
        addChainRow(sheet3, cRow++, "存证类型", "Keccak-256 Hash");
        addChainRow(sheet3, cRow++, "链上服务状态", chainAvailable);
        addChainRow(sheet3, cRow++, "验证方式", "预期Hash与链上Hash比对");
        addChainRow(sheet3, cRow++, "数据完整性", verifyStatus.indexOf("PASS") >= 0 ? "✅ 完整（Hash一致）"
                : verifyStatus.indexOf("MISMATCH") >= 0 ? "❌ 不一致（可能被篡改）"
                : verifyStatus.indexOf("NO_RECORD") >= 0 ? "⚠️ 链上无存证记录" : "⚠️ 验证异常");

        cRow++;
        Row secNote = sheet3.createRow(cRow++);
        Cell noteCell = secNote.createCell(0);
        noteCell.setCellValue("说明：Hash核对基于 SHA-256/Keccak-256 算法，链上存证不可篡改。预期Hash由数据库当前内容计算，实际Hash从区块链合约读取。两者一致表明数据未被篡改。");
        noteCell.setCellStyle(leftStyle);
        sheet3.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(cRow - 2, cRow - 1, 0, 1));

        // ---------- Sheet4：验证结论 ----------
        Sheet sheet4 = wb.createSheet("验证结论");
        sheet4.setColumnWidth(0, 6000);
        sheet4.setColumnWidth(1, 12000);

        int cRow4 = 0;
        Row concTitle = sheet4.createRow(cRow4++);
        Cell concCell = concTitle.createCell(0);
        concCell.setCellValue("验证结论");
        concCell.setCellStyle(titleStyle);
        sheet4.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

        cRow4++;
        String conclusion = "";
        String conclusionDetail = "";
        if (verifyStatus.indexOf("PASS") >= 0) {
            conclusion = "✅ 验证通过";
            conclusionDetail = "链上存证Hash与数据库内容Hash一致，证明该身份的凭证信息未被篡改，链上存证真实有效。";
        } else if (verifyStatus.indexOf("MISMATCH") >= 0) {
            conclusion = "❌ 验证不通过";
            conclusionDetail = "链上存证Hash与数据库内容Hash不一致，可能存在数据被篡改的风险，请立即核查。";
        } else if (verifyStatus.indexOf("NO_RECORD") >= 0) {
            conclusion = "⚠️ 无链上记录";
            conclusionDetail = "该身份或操作尚未上链，或链上存证已被清除。请确认是否已完成注册上链流程。";
        } else {
            conclusion = "⚠️ 验证异常";
            conclusionDetail = "无法完成链上验证，可能原因：链服务未配置、网络异常或数据缺失。";
        }

        addConcRow(sheet4, cRow4++, "结论状态", conclusion);
        addConcRow(sheet4, cRow4++, "结论说明", conclusionDetail);
        addConcRow(sheet4, cRow4++, "DID", verifyTarget);
        addConcRow(sheet4, cRow4++, "交易哈希", txHashValue);
        addConcRow(sheet4, cRow4++, "验证时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        addConcRow(sheet4, cRow4++, "报告生成时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        cRow4++;
        Row sigRow = sheet4.createRow(cRow4++);
        Cell sigCell = sigRow.createCell(0);
        sigCell.setCellValue("本报告由系统自动生成，仅供核实数据完整性参考使用。");
        sigCell.setCellStyle(leftStyle);
        sheet4.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(cRow4 - 1, cRow4 - 1, 0, 1));

        // 自动调整列宽
        for (int s = 0; s < wb.getNumberOfSheets(); s++) {
            Sheet sht = wb.getSheetAt(s);
            for (int i = 0; i < 3; i++) {
                sht.autoSizeColumn(i);
            }
        }

        wb.write(out);
        wb.close();

        // 返回文件下载
        String filename = "verify_report_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.getOutputStream().write(out.toByteArray());
        response.getOutputStream().flush();
    }

    private void addSummaryRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value != null ? value : "—");
    }

    private void addHashRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        Cell valCell = row.createCell(1);
        valCell.setCellValue(value != null ? value : "—");
    }

    private void addAuditRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value != null ? value : "—");
    }

    private void addChainRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value != null ? value : "—");
    }

    private void addConcRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        Cell valCell = row.createCell(1);
        valCell.setCellValue(value != null ? value : "—");
    }

    private String getVerifyStatusText(String expected, String actual) {
        if (actual == null || actual.isEmpty()) return "⚠️ 无链上记录";
        if (expected == null || expected.isEmpty()) return "❌ 验证异常";
        if (expected.equals(actual)) return "✅ Hash一致";
        return "❌ Hash不一致";
    }

    private String getChainHashByKey(String key) {
        if (web3j == null || credentials == null || gasProvider == null
                || contractAddress == null || contractAddress.trim().isEmpty()) {
            return null;
        }
        try {
            String hash = EvidenceStorageContract.get(web3j, credentials, gasProvider, contractAddress.trim(), key);
            return hash != null ? hash.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getCurrentBlockNumber() {
        if (web3j == null) return "链服务不可用";
        try {
            BigInteger bn = web3j.getBlockNumber().send().getBlockNumber();
            return bn != null ? bn.toString() : "未知";
        } catch (Exception e) {
            return "获取失败";
        }
    }
}
