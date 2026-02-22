package com.test.Entity;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DID 身份实体（方案A：应用生成 DID，存库；下一阶段可扩展链上存证字段）
 */
@Data
@TableName("did_identity")
public class Identity {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 工号 */
    private String employeeId;
    /** 姓名 */
    private String name;
    /** 部门 */
    private String department;
    /** 职位 */
    private String position;
    /** 权限角色：员工、超级管理员 */
    private String role;
    /** 去中心化标识符 did:blockdia:emp:xxx */
    private String did;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 上链交易哈希（下一阶段使用） */
    private String chainTxHash;
    /** 上链区块号（下一阶段使用） */
    private Long chainBlockNumber;

    /** 存证/验证时 createdAt 的格式（只到秒，与 DB 读出一致，避免注册时带纳秒导致 hash 不一致） */
    public static final DateTimeFormatter HASH_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 用于链上存证与验证的拼接字符串，仅含 did|employeeId|name|department|position|role|createdAt，
     * 不含 id、chainTxHash、chainBlockNumber；两处共用以保证 hash 一致。
     */
    public static String buildContentString(Identity i) {
        if (i == null) return "";
        String createdAtStr = i.getCreatedAt() != null ? i.getCreatedAt().format(HASH_TIMESTAMP_FORMAT) : "";
        return i.getDid() + "|" + i.getEmployeeId() + "|" + i.getName()
                + "|" + (i.getDepartment() != null ? i.getDepartment() : "")
                + "|" + (i.getPosition() != null ? i.getPosition() : "")
                + "|" + (i.getRole() != null ? i.getRole() : "")
                + "|" + createdAtStr;
    }

    /** 与 buildContentString 配套：用 UTF-8 对 content 做 SHA256 得到 hex，注册与验证共用。 */
    public static String computeContentHash(Identity i) {
        if (i == null) return "";
        byte[] bytes = buildContentString(i).getBytes(StandardCharsets.UTF_8);
        return DigestUtil.sha256Hex(bytes);
    }
}
