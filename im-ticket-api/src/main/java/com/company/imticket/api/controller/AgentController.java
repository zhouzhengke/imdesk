package com.company.imticket.api.controller;

import com.company.imticket.common.dto.PageResp;
import com.company.imticket.common.dto.R;
import com.company.imticket.dao.entity.Agent;
import com.company.imticket.dao.mapper.AgentMapper;
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
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentMapper agentMapper;

    public AgentController(AgentMapper agentMapper) {
        this.agentMapper = agentMapper;
    }

    @GetMapping
    public R<PageResp<Agent>> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String status) {
        Page<Agent> p = new Page<>(page, size);
        IPage<Agent> result;
        if (status != null && !status.isEmpty()) {
            result = agentMapper.selectPage(p,
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Agent>()
                            .eq(Agent::getStatus, status));
        } else {
            result = agentMapper.selectPage(p, null);
        }
        PageResp<Agent> resp = new PageResp<>();
        resp.setRecords(result.getRecords());
        resp.setTotal(result.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return R.ok(resp);
    }

    @GetMapping("/{id}")
    public R<Agent> getById(@PathVariable Long id) {
        return R.ok(agentMapper.selectById(id));
    }

    @PostMapping
    public R<Agent> create(@RequestBody Agent agent) {
        agentMapper.insert(agent);
        return R.ok(agent);
    }

    @PutMapping("/{id}")
    public R<Agent> update(@PathVariable Long id, @RequestBody Agent agent) {
        agent.setId(id);
        agentMapper.updateById(agent);
        return R.ok(agent);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        agentMapper.deleteById(id);
        return R.ok(null);
    }

    @PutMapping("/{id}/status")
    public R<Agent> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Agent agent = agentMapper.selectById(id);
        agent.setStatus(status);
        agentMapper.updateById(agent);
        return R.ok(agent);
    }
}
