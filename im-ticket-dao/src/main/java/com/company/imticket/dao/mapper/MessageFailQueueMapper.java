package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.imticket.dao.entity.MessageFailQueue;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息失败队列表 Mapper
 */
@Mapper
public interface MessageFailQueueMapper extends BaseMapper<MessageFailQueue> {
}