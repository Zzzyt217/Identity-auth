-- 请先在 MySQL 中执行本脚本（仅需执行一次），创建数据库 identity_db。
-- 应用启动时会自动执行 schema.sql，在 identity_db 中创建表 did_identity。
CREATE DATABASE IF NOT EXISTS identity_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
