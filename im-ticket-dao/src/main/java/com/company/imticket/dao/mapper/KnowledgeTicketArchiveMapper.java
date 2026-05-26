package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.imticket.dao.entity.KnowledgeTicketArchive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 历史工单归档表 Mapper
 */
@Mapper
public interface KnowledgeTicketArchiveMapper extends BaseMapper<KnowledgeTicketArchive> {
}