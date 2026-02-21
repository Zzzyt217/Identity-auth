-- 已有库时执行：创建审计日志表（仅需执行一次）
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
