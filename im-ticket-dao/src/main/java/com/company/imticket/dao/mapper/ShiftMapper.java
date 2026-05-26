package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.imticket.dao.entity.Shift;
import org.apache.ibatis.annotations.Mapper;

/**
 * 值班班次表 Mapper
 */
@Mapper
public interface ShiftMapper extends BaseMapper<Shift> {
}