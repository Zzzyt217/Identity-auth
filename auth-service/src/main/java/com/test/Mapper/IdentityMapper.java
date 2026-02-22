package com.test.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.test.Entity.Identity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdentityMapper extends BaseMapper<Identity> {
}
