package com.test.config;

import com.test.Entity.BackupRecord;
import com.test.Service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;

/**
 * 数据库自动定时备份配置
 */
@Configuration
public class BackupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BackupScheduler.class);

    @Resource
    private BackupService backupService;

    @Value("${backup.auto.enabled:true}")
    private boolean autoBackupEnabled;

    @Value("${backup.auto.cleanup-enabled:true}")
    private boolean autoCleanupEnabled;


    @Scheduled(cron = "${backup.auto.cron:0 0 17 * * ?}")
    public void scheduledBackup() {
        if (!autoBackupEnabled) {
            logger.info("自动备份已禁用，跳过本次备份");
            return;
        }

        logger.info("========== 开始执行定时数据库备份 ==========");
        long startTime = System.currentTimeMillis();

        try {
            BackupRecord record = backupService.backup();

            if ("SUCCESS".equals(record.getStatus())) {
                logger.info("定时备份成功！文件: {}, 大小: {} bytes, 耗时: {}秒",
                        record.getFilePath(),
                        record.getFileSize(),
                        record.getDurationSeconds());
            } else {
                logger.error("定时备份失败: {}", record.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("定时备份异常", e);
        }

        logger.info("========== 定时备份完成，耗时: {} ms ==========",
                System.currentTimeMillis() - startTime);
    }

    /**
     * 每天凌晨 3:00 清理过期备份文件
     */
    @Scheduled(cron = "${backup.auto.cleanup-cron:0 0 3 * * ?}")
    public void scheduledCleanup() {
        if (!autoCleanupEnabled) {
            logger.info("自动清理已禁用，跳过本次清理");
            return;
        }

        logger.info("========== 开始清理过期备份文件 ==========");

        try {
            int cleanedCount = backupService.cleanupOldBackups(30);
            logger.info("清理完成，共删除 {} 个过期备份文件", cleanedCount);
        } catch (Exception e) {
            logger.error("清理过期备份异常", e);
        }

        logger.info("========== 清理过期备份完成 ==========");
    }
}
