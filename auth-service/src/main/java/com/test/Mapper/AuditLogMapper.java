package com.test.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.test.Entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
