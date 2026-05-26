package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资方渠道映射表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_capital_channel")
public class CapitalChannelMapping extends BaseEntity {

    private Long capitalId;

    private String channel;

    private String identifierType;

    private String identifierValue;
}