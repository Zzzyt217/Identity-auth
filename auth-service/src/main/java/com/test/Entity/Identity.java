package com.test.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

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

}
