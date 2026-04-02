package com.test.Service;

import com.test.Entity.BackupRecord;
import com.test.Entity.BackupRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据库备份服务接口
 */
public interface BackupService {

    /** 备份类型：完整备份 */
    String BACKUP_TYPE_FULL = "FULL";

    /** 备份状态：成功 */
    String STATUS_SUCCESS = "SUCCESS";

    /** 备份状态：失败 */
    String STATUS_FAILED = "FAILED";

    /**
     * 执行数据库备份
     * @return 备份记录
     */
    BackupRecord backup();

    /**
     * 查询所有备份记录
     * @return 备份记录列表
     */
    List<BackupRecord> listAll();

    /**
     * 根据 ID 查询备份记录
     * @param id 记录ID
     * @return 备份记录
     */
    BackupRecord getById(Long id);

    /**
     * 删除备份文件及记录
     * @param id 记录ID
     * @return 是否删除成功
     */
    boolean delete(Long id);

    /**
     * 获取备份文件存储目录
     * @return 备份目录路径
     */
    String getBackupDirectory();

    /**
     * 清理过期备份文件（保留最近 N 天）
     * @param retentionDays 保留天数
     * @return 清理的文件数量
     */
    int cleanupOldBackups(int retentionDays);

    /**
     * 获取备份统计信息
     * @return 统计信息
     */
    Map<String, Object> getStatistics();
}
