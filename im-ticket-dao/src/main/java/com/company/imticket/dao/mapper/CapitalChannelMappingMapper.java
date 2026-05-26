package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.imticket.dao.entity.CapitalChannelMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资方渠道映射表 Mapper
 */
@Mapper
public interface CapitalChannelMappingMapper extends BaseMapper<CapitalChannelMapping> {

    @Select("SELECT * FROM im_capital_channel WHERE channel = #{channel} AND identifier_value = #{identifierValue} AND deleted = 0 LIMIT 1")
    CapitalChannelMapping findByChannelAndIdentifier(@Param("channel") String channel, @Param("identifierValue") String identifierValue);
}