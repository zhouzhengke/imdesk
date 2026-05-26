package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 排班表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_shift_schedule")
public class ShiftSchedule extends BaseEntity {

    private LocalDate scheduleDate;

    private Long shiftId;

    private String agentIds;
}