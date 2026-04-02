-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: localhost    Database: identity_db
-- ------------------------------------------------------
-- Server version	8.0.33

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `audit_log`
--

DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operation_type` varchar(32) NOT NULL COMMENT '操作类型：UPDATE_ROLE-权限修改，REVOKE_DID-注销DID',
  `target_identity_id` bigint DEFAULT NULL,
  `target_did` varchar(256) DEFAULT NULL,
  `target_employee_id` varchar(64) DEFAULT NULL,
  `target_name` varchar(64) DEFAULT NULL,
  `detail` varchar(512) DEFAULT NULL COMMENT '操作说明',
  `operated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `chain_tx_hash` varchar(128) DEFAULT NULL COMMENT '上链交易哈希（下一阶段使用）',
  `chain_block_number` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_operated_at` (`operated_at`),
  KEY `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_log`
--

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
INSERT INTO `audit_log` VALUES (1,'UPDATE_ROLE',1,'did:blockdia:emp:1001','1001','炸油条','员工 -> 超级管理员','2026-02-21 16:42:55',NULL,NULL),(2,'REVOKE_DID',6,'did:blockdia:emp:1006','1006','大宝','注销DID','2026-02-21 16:43:03',NULL,NULL),(3,'UPDATE_ROLE',7,'did:blockdia:emp:1006','1006','小宝','员工 -> 超级管理员','2026-02-21 17:01:41',NULL,NULL),(4,'UPDATE_ROLE',8,'did:blockdia:emp:1007','1007','大宝','超级管理员 -> 员工','2026-02-22 09:39:42',NULL,NULL),(5,'UPDATE_ROLE',10,'did:blockdia:emp:1010','1010','小小怪','超级管理员 -> 员工','2026-02-22 15:24:22','0xb8c32c1a7c4720248b0bb8c183091154ab34d30131ca7715605998dd601f2032',8),(6,'REVOKE_DID',4,'did:blockdia:emp:1004','1004','披风','注销DID','2026-02-22 15:40:25','0xc1551001eda9fbb9ac6f6f4fce938419c6ddff29bce048d4e66f8b25b0be7622',9),(7,'REVOKE_DID',13,'did:blockdia:emp:1013','1013','光头强','注销DID','2026-02-22 17:05:27','0x0ebd06062b74417ae34071a91ec6d02300a852460d544a494e4237a56dcfca25',15),(8,'REVOKE_DID',11,'did:blockdia:emp:1011','1011','熊大','注销DID','2026-02-22 17:05:40','0x0524572f8f0fe143150848396602f40f3fbd8b525097bba549a8efb2fad9ca4e',16),(9,'UPDATE_ROLE',1,'did:blockdia:emp:1001','1001','炸油条','超级管理员 -> 员工','2026-02-22 17:10:44','0x626ec81c46770ea050e7883348e86ce1283f29f5b0d5d2f68d208d93a2e69bba',17),(10,'UPDATE_ROLE',14,'did:blockdia:emp:1014','1014','翠花111','员工 -> 超级管理员','2026-02-23 10:13:47','0xc4cf34e57c23453f8e92898c33beb3451d02e2a3ae24bf01c369a172772484cb',19),(11,'UPDATE_ROLE',15,'did:blockdia:emp:1015','1015','毛毛','员工 -> 超级管理员','2026-02-23 10:14:07','0x8e98605968e7d4021bd8c78a1afc21d364f494d50ef8d342d22c87696f0c6e9d',20),(12,'UPDATE_ROLE',15,'did:blockdia:emp:1015','1015','毛毛','超级管理员 -> 员工','2026-02-23 10:14:28','0xe5729e7a50e9462948dd3e8a6de3a81af6b22f874cccbcb5e111467ffce7c3d0',21),(13,'UPDATE_ROLE',1,'did:blockdia:emp:1001','1001','炸油条','员工 -> 超级管理员','2026-02-23 15:59:12','0x11252a1db47ad44fdddabdbe17160ee493586f9f099dc865020d33e9ec18f7e8',24),(14,'UPDATE_ROLE',5,'did:blockdia:emp:1005','1005','麒麟','员工 -> 员工','2026-02-23 16:08:56','0x0bccf9f888e291866921da60c495798ad2a0d7e886a2e24f108c959ed6c1833a',25),(15,'UPDATE_ROLE',7,'did:blockdia:emp:1006','1006','小宝','超级管理员 -> 员工','2026-02-23 16:09:49','0x4f736871099e8c6d68c8072f59d4a478a4032632849806844a7d7e071ec0c7bf',26),(16,'REVOKE_DID',3,'did:blockdia:emp:1003','1003','咸鱼111','注销DID','2026-02-23 16:18:07','0x7015c9792880356c83efbb05a8e26b6e1ae472b19628af2712b7410537953ef7',27),(17,'UPDATE_ROLE',14,'did:blockdia:emp:1014','1014','翠花111','超级管理员 -> 员工','2026-02-23 16:25:31','0xa51f165e297ba9f27678e0cc3021d992fe6b9139a0d4982a7156e0db641d33ad',28),(18,'UPDATE_ROLE',10,'did:blockdia:emp:1010','1010','小小怪','员工 -> 超级管理员','2026-03-20 11:42:33','0x1ca7be382401ea1580478955db5c2f931e65bb2bbed0d4a6462a4c248a780f81',32),(19,'REVOKE_DID',9,'did:blockdia:emp:1009','1009','大大怪','注销DID','2026-03-20 11:43:40','0x7df99fe62419089f2c7ea7f3b6d67cfd21d0e68d0a510adb3fdb0eb2f4a38e14',33),(20,'REVOKE_DID',10,'did:blockdia:emp:1010','1010','小小怪','注销DID','2026-03-20 11:43:45','0x2dad26b95e7aacb7a5d38143c893b2a6e3d3df8dc2846d93298fbe0590d0ff8f',34),(21,'UPDATE_ROLE',1,'did:blockdia:emp:1001','1001','炸油条','超级管理员 -> 员工','2026-03-20 11:43:50','0xa1aa16b36c4961492686e6ec4d5fcf227485e2aa7a8e78cd1325f58ffb6f7626',35),(22,'REVOKE_DID',5,'did:blockdia:emp:1005','1005','麒麟','注销DID','2026-03-26 12:12:43','0xe47c1f3d701553cafcfe41bd4bbd51b4f67bcaf8ea232b7e34494bfda98c75c8',36),(23,'REVOKE_DID',26,'did:blockdia:emp:1028','1028','zz','注销DID','2026-04-02 16:41:04','0x8e35c24e9940699c72bc29882ecd5383f6e72452602721a5471715ed445d93b9',43);
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `backup_record`
--

DROP TABLE IF EXISTS `backup_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `backup_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `backup_type` varchar(20) NOT NULL DEFAULT 'FULL',
  `file_path` varchar(512) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `backup_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(20) NOT NULL,
  `duration_seconds` int DEFAULT NULL,
  `error_message` varchar(1024) DEFAULT NULL,
  `operator` varchar(50) DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_backup_time` (`backup_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `backup_record`
--

LOCK TABLES `backup_record` WRITE;
/*!40000 ALTER TABLE `backup_record` DISABLE KEYS */;
INSERT INTO `backup_record` VALUES (12,'FULL',NULL,NULL,'2026-04-01 14:53:27','FAILED',0,'\'D:/MySQL/MySQL\' �����ڲ����ⲿ���Ҳ���ǿ����еĳ���\r\n���������ļ���','SYSTEM',NULL),(13,'FULL','./backups\\identity_db_20260401_145803.sql',12070,'2026-04-01 14:58:03','SUCCESS',0,NULL,'SYSTEM',NULL);
/*!40000 ALTER TABLE `backup_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `did_identity`
--

DROP TABLE IF EXISTS `did_identity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `did_identity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` varchar(64) NOT NULL COMMENT '工号',
  `name` varchar(64) NOT NULL COMMENT '姓名',
  `department` varchar(64) NOT NULL COMMENT '部门',
  `position` varchar(128) DEFAULT NULL COMMENT '职位',
  `role` varchar(32) NOT NULL DEFAULT '员工' COMMENT '权限角色',
  `did` varchar(256) NOT NULL COMMENT '去中心化标识符',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `chain_tx_hash` varchar(128) DEFAULT NULL COMMENT '上链交易哈希',
  `chain_block_number` bigint DEFAULT NULL COMMENT '上链区块号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_id` (`employee_id`),
  UNIQUE KEY `uk_did` (`did`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DID身份表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `did_identity`
--

LOCK TABLES `did_identity` WRITE;
/*!40000 ALTER TABLE `did_identity` DISABLE KEYS */;
INSERT INTO `did_identity` VALUES (1,'1001','炸油条','技术部','工程师','员工','did:blockdia:emp:1001','2026-02-21 16:27:06',NULL,NULL),(7,'1006','小宝','技术部','工程师','员工','did:blockdia:emp:1006','2026-02-21 17:01:29',NULL,NULL),(8,'1007','大宝','技术部','工程师','员工','did:blockdia:emp:1007','2026-02-22 09:39:22',NULL,NULL),(12,'1012','熊二','市场部','营销','员工','did:blockdia:emp:1012','2026-02-22 16:33:23','0xb25f2ad8b92016d22f5e7ce93d5c1a276bb6d60b80c49dcf7165033529539494',11),(14,'1014','翠花111','市场部','营销','员工','did:blockdia:emp:1014','2026-02-22 16:55:42','0xaf8d61aae30d03857f55a6ef10e579da3594301da259f20d0a7d829d086bcf63',13),(15,'1015','毛毛','运营部','媒体运营','员工','did:blockdia:emp:1015','2026-02-22 17:03:28','0xa89151df95f3b89ec8c148e91906976e3506d2c958cced3f953177e958e5a310',14),(17,'1016','吉吉','人事部','HR','员工','did:blockdia:emp:1016','2026-02-23 10:08:29','0x8896bbf9adde91957998d2abbd8af353ea5395b36efdf442eaebbe5d9d43002e',18),(18,'10017','大饼','技术部','工程师','员工','did:blockdia:emp:10017','2026-02-23 15:45:58','0x663afd72a3c70376da3148a50818928e6a65ef36f93f1d4300783a52a2051636',22),(19,'1017','小饼','技术部','工程师','员工','did:blockdia:emp:1017','2026-02-23 15:58:41','0x99ca9c24196ae986261c402fdd9ac6bf3272c43085792e16ed9d2cba71178865',23),(20,'1024','cc','技术部','工程师','员工','did:blockdia:emp:1024','2026-03-17 10:15:24','0xc5b44334bd614ae2213141e2488e9cdc3fc42485d4c8c93fc17147c5a8698163',29),(21,'1018','大鹏','财务部','会计','员工','did:blockdia:emp:1018','2026-03-19 14:52:37','0xb57713b16059f538795a75acc569fce65dac351cc0df21e50d07c0d22ffad00d',30),(22,'1019','大鹏','产品部','销售经理','员工','did:blockdia:emp:1019','2026-03-20 11:38:59','0x245f8610e1917cb9ade5f8172034e6be33b8dd800e4b24c3ded914d34b65acc4',31),(23,'1025','bb','技术部','工程师','员工','did:blockdia:emp:1025','2026-04-01 13:26:33','0xa7daa199e53a762b82d5137f4ce88b402d91ed757f22d747e435acc8b5e2b7f7',39),(24,'1026','dd','人事部','工程师','员工','did:blockdia:emp:1026','2026-04-01 13:36:09','0xb5a338fa3a137b0ee515669db40ac7e292fdb1adb6cbdf252ea8738815b1f3f3',40),(25,'1027','ff','人事部','会计','员工','did:blockdia:emp:1027','2026-04-01 13:41:28','0xb393bb7bc7afd5716129b44cd1f42457365fb5bbca228dc5d4d95a0b16712326',41);
/*!40000 ALTER TABLE `did_identity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'identity_db'
--

--
-- Dumping routines for database 'identity_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-02 17:01:31
