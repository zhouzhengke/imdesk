package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 值班班次表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_shift")
public class Shift extends BaseEntity {

    private String name;

    private LocalTime startTime;

    private LocalTime endTime;

    private String dutyType;

    private String primaryAgentIds;

    private String backupAgentIds;
}