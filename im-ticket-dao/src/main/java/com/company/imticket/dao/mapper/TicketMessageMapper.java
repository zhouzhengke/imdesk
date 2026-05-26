package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.imticket.dao.entity.TicketMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单消息表 Mapper
 */
@Mapper
public interface TicketMessageMapper extends BaseMapper<TicketMessage> {

    @Select("SELECT * FROM im_ticket_message WHERE ticket_id = #{ticketId} ORDER BY created_at ASC")
    List<TicketMessage> selectByTicketId(@Param("ticketId") Long ticketId);
}