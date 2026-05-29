package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.imticket.dao.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工单表 Mapper
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    IPage<Ticket> selectPageWithFilters(Page<Ticket> page,
                                        @Param("channel") String channel,
                                        @Param("capitalId") Long capitalId,
                                        @Param("status") String status,
                                        @Param("agentId") Long agentId,
                                        @Param("keyword") String keyword);

    @Select("SELECT * FROM im_ticket WHERE channel_user_id = #{channelUserId} AND channel = #{channel} AND status NOT IN ('CLOSED') AND deleted = 0 LIMIT 1")
    Ticket findOpenTicketByChannelUser(@Param("channel") String channel, @Param("channelUserId") String channelUserId);

    @Select("SELECT * FROM im_ticket WHERE channel_user_id = #{channelUserId} AND channel = #{channel} AND status NOT IN ('CLOSED') AND deleted = 0 LIMIT 1 FOR UPDATE")
    Ticket findOpenTicketByChannelUserForUpdate(@Param("channel") String channel, @Param("channelUserId") String channelUserId);

    @Select("SELECT COUNT(*) FROM im_ticket WHERE status = 'PENDING' AND deleted = 0")
    Long countPendingTickets();
}