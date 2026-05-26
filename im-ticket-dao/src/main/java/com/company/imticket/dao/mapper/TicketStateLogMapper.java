package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.imticket.dao.entity.TicketStateLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单状态流水表 Mapper
 */
@Mapper
public interface TicketStateLogMapper extends BaseMapper<TicketStateLog> {
}