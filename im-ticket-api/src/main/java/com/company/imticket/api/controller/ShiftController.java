package com.company.imticket.api.controller;

import com.company.imticket.common.dto.PageResp;
import com.company.imticket.common.dto.R;
import com.company.imticket.dao.entity.Shift;
import com.company.imticket.dao.entity.ShiftSchedule;
import com.company.imticket.dao.mapper.ShiftMapper;
import com.company.imticket.dao.mapper.ShiftScheduleMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ShiftController {

    private final ShiftMapper shiftMapper;
    private final ShiftScheduleMapper scheduleMapper;

    public ShiftController(ShiftMapper shiftMapper, ShiftScheduleMapper scheduleMapper) {
        this.shiftMapper = shiftMapper;
        this.scheduleMapper = scheduleMapper;
    }

    @GetMapping("/shifts")
    public R<PageResp<Shift>> listShifts(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        IPage<Shift> p = shiftMapper.selectPage(new Page<>(page, size), null);
        PageResp<Shift> resp = new PageResp<>();
        resp.setRecords(p.getRecords());
        resp.setTotal(p.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return R.ok(resp);
    }

    @PostMapping("/shifts")
    public R<Shift> createShift(@RequestBody Shift shift) {
        shiftMapper.insert(shift);
        return R.ok(shift);
    }

    @PutMapping("/shifts/{id}")
    public R<Shift> updateShift(@PathVariable Long id, @RequestBody Shift shift) {
        shift.setId(id);
        shiftMapper.updateById(shift);
        return R.ok(shift);
    }

    @DeleteMapping("/shifts/{id}")
    public R<Void> deleteShift(@PathVariable Long id) {
        shiftMapper.deleteById(id);
        return R.ok(null);
    }

    @GetMapping("/schedules")
    public R<PageResp<ShiftSchedule>> listSchedules(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        IPage<ShiftSchedule> p = scheduleMapper.selectPage(new Page<>(page, size), null);
        PageResp<ShiftSchedule> resp = new PageResp<>();
        resp.setRecords(p.getRecords());
        resp.setTotal(p.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return R.ok(resp);
    }

    @PostMapping("/schedules")
    public R<ShiftSchedule> createSchedule(@RequestBody ShiftSchedule schedule) {
        scheduleMapper.insert(schedule);
        return R.ok(schedule);
    }

    @DeleteMapping("/schedules/{id}")
    public R<Void> deleteSchedule(@PathVariable Long id) {
        scheduleMapper.deleteById(id);
        return R.ok(null);
    }
}
