package com.test.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库备份记录实体
 */
@Data
@TableName("backup_record")
public class BackupRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 备份类型：FULL（完整备份） */
    private String backupType;

    /** 备份文件路径 */
    private String filePath;

    /** 备份文件大小（字节） */
    private Long fileSize;

    /** 备份时间 */
    private LocalDateTime backupTime;

    /** 备份状态：SUCCESS、FAILED */
    private String status;

    /** 备份耗时（秒） */
    private Integer durationSeconds;

    /** 错误信息（备份失败时记录） */
    private String errorMessage;

    /** 操作人 */
    private String operator;

    /** 备注 */
    private String remark;
}
