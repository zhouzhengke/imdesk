package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.imticket.dao.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客服表 Mapper
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}