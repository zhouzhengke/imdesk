package com.company.imticket.api.controller;

import com.company.imticket.common.dto.PageResp;
import com.company.imticket.common.dto.R;
import com.company.imticket.dao.entity.Capital;
import com.company.imticket.dao.mapper.CapitalMapper;
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
@RequestMapping("/api/v1/capitals")
public class CapitalController {

    private final CapitalMapper capitalMapper;

    public CapitalController(CapitalMapper capitalMapper) {
        this.capitalMapper = capitalMapper;
    }

    @GetMapping
    public R<PageResp<Capital>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        IPage<Capital> p = capitalMapper.selectPage(new Page<>(page, size), null);
        PageResp<Capital> resp = new PageResp<>();
        resp.setRecords(p.getRecords());
        resp.setTotal(p.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return R.ok(resp);
    }

    @GetMapping("/{id}")
    public R<Capital> getById(@PathVariable Long id) {
        return R.ok(capitalMapper.selectById(id));
    }

    @PostMapping
    public R<Capital> create(@RequestBody Capital capital) {
        capitalMapper.insert(capital);
        return R.ok(capital);
    }

    @PutMapping("/{id}")
    public R<Capital> update(@PathVariable Long id, @RequestBody Capital capital) {
        capital.setId(id);
        capitalMapper.updateById(capital);
        return R.ok(capital);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        capitalMapper.deleteById(id);
        return R.ok(null);
    }
}
