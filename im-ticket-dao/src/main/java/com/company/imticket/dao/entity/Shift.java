package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalTime;

@TableName("im_shift")
public class Shift extends BaseEntity {

    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private String dutyType;
    private String primaryAgentIds;
    private String backupAgentIds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getDutyType() { return dutyType; }
    public void setDutyType(String dutyType) { this.dutyType = dutyType; }
    public String getPrimaryAgentIds() { return primaryAgentIds; }
    public void setPrimaryAgentIds(String primaryAgentIds) { this.primaryAgentIds = primaryAgentIds; }
    public String getBackupAgentIds() { return backupAgentIds; }
    public void setBackupAgentIds(String backupAgentIds) { this.backupAgentIds = backupAgentIds; }
}