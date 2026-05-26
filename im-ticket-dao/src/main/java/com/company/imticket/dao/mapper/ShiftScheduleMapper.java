package com.company.imticket.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.imticket.dao.entity.ShiftSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 排班表 Mapper
 */
@Mapper
public interface ShiftScheduleMapper extends BaseMapper<ShiftSchedule> {

    @Select("SELECT ss.agent_ids FROM im_shift_schedule ss JOIN im_shift s ON ss.shift_id = s.id WHERE ss.schedule_date = #{date} AND s.start_time <= #{time} AND s.end_time > #{time} AND s.deleted = 0 AND ss.deleted = 0 LIMIT 1")
    String findCurrentDutyAgents(@Param("date") LocalDate date, @Param("time") LocalTime time);

    @Select("SELECT s.backup_agent_ids FROM im_shift_schedule ss JOIN im_shift s ON ss.shift_id = s.id WHERE ss.schedule_date = #{date} AND s.start_time <= #{time} AND s.end_time > #{time} AND s.deleted = 0 AND ss.deleted = 0 LIMIT 1")
    String findCurrentBackupAgents(@Param("date") LocalDate date, @Param("time") LocalTime time);
}