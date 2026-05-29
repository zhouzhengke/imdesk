package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

@TableName("im_shift_schedule")
public class ShiftSchedule extends BaseEntity {

    private LocalDate scheduleDate;
    private Long shiftId;
    private String agentIds;

    public LocalDate getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }
    public String getAgentIds() { return agentIds; }
    public void setAgentIds(String agentIds) { this.agentIds = agentIds; }
}