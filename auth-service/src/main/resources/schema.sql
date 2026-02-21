-- 方案A：DID 身份表（当前阶段仅落库，下一阶段可增加链上存证字段）
-- 表名使用 did_identity 避免与 MySQL 保留字 identity 冲突
CREATE TABLE IF NOT EXISTS `did_identity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` varchar(64) NOT NULL COMMENT '工号',
  `name` varchar(64) NOT NULL COMMENT '姓名',
  `department` varchar(64) NOT NULL COMMENT '部门',
  `position` varchar(128) DEFAULT NULL COMMENT '职位',
  `role` varchar(32) NOT NULL DEFAULT '员工' COMMENT '权限角色：员工、超级管理员',
  `did` varchar(256) NOT NULL COMMENT '去中心化标识符 did:blockdia:emp:xxx',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `chain_tx_hash` varchar(128) DEFAULT NULL COMMENT '上链交易哈希（下一阶段使用）',
  `chain_block_number` bigint DEFAULT NULL COMMENT '上链区块号（下一阶段使用）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_id` (`employee_id`),
  UNIQUE KEY `uk_did` (`did`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DID身份表（方案A）';

-- 审计日志表（权限修改、注销DID；下一阶段可填充 chain_tx_hash 用于链上验证）
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型：UPDATE_ROLE-权限修改，REVOKE_DID-注销DID',
  `target_identity_id` bigint DEFAULT NULL COMMENT '目标身份主键',
  `target_did` varchar(256) DEFAULT NULL COMMENT '目标DID',
  `target_employee_id` varchar(64) DEFAULT NULL COMMENT '目标工号',
  `target_name` varchar(64) DEFAULT NULL COMMENT '目标姓名',
  `detail` varchar(512) DEFAULT NULL COMMENT '操作说明，如：员工 -> 超级管理员、注销DID',
  `operated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `chain_tx_hash` varchar(128) DEFAULT NULL COMMENT '上链交易哈希（下一阶段使用）',
  `chain_block_number` bigint DEFAULT NULL COMMENT '上链区块号（下一阶段使用）',
  PRIMARY KEY (`id`),
  KEY `idx_operated_at` (`operated_at`),
  KEY `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
