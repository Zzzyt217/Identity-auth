package com.test.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.test.Entity.BackupRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 备份记录 Mapper
 */
@Mapper
public interface BackupRecordMapper extends BaseMapper<BackupRecord> {
}
