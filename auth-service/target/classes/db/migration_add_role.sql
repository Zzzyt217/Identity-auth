-- 已有表时执行：为 did_identity 增加权限角色字段（仅需执行一次）
ALTER TABLE `did_identity` ADD COLUMN `role` varchar(32) NOT NULL DEFAULT '员工' COMMENT '权限角色：员工、超级管理员' AFTER `position`;
