package com.company.imticket.api.controller;

import com.company.imticket.common.dto.PageResp;
import com.company.imticket.common.dto.R;
import com.company.imticket.dao.entity.NotificationTemplate;
import com.company.imticket.dao.mapper.NotificationTemplateMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-templates")
public class NotificationTemplateController {

    private final NotificationTemplateMapper templateMapper;

    public NotificationTemplateController(NotificationTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @GetMapping
    public R<PageResp<NotificationTemplate>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        IPage<NotificationTemplate> p = templateMapper.selectPage(new Page<>(page, size), null);
        PageResp<NotificationTemplate> resp = new PageResp<>();
        resp.setRecords(p.getRecords());
        resp.setTotal(p.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return R.ok(resp);
    }

    @PutMapping("/{id}")
    public R<NotificationTemplate> update(@PathVariable Long id, @RequestBody NotificationTemplate template) {
        template.setId(id);
        templateMapper.updateById(template);
        return R.ok(template);
    }
}
