package com.company.imticket.api.controller;

import com.company.imticket.common.dto.PageResp;
import com.company.imticket.common.dto.R;
import com.company.imticket.dao.entity.KnowledgeDocument;
import com.company.imticket.dao.entity.KnowledgeFaq;
import com.company.imticket.dao.mapper.KnowledgeDocumentMapper;
import com.company.imticket.dao.mapper.KnowledgeFaqMapper;
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
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeFaqMapper faqMapper;
    private final KnowledgeDocumentMapper docMapper;

    public KnowledgeController(KnowledgeFaqMapper faqMapper, KnowledgeDocumentMapper docMapper) {
        this.faqMapper = faqMapper;
        this.docMapper = docMapper;
    }

    @GetMapping("/faqs")
    public R<PageResp<KnowledgeFaq>> listFaqs(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        IPage<KnowledgeFaq> p = faqMapper.selectPage(new Page<>(page, size), null);
        PageResp<KnowledgeFaq> resp = new PageResp<>();
        resp.setRecords(p.getRecords());
        resp.setTotal(p.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return R.ok(resp);
    }

    @PostMapping("/faqs")
    public R<KnowledgeFaq> createFaq(@RequestBody KnowledgeFaq faq) {
        faqMapper.insert(faq);
        return R.ok(faq);
    }

    @PutMapping("/faqs/{id}")
    public R<KnowledgeFaq> updateFaq(@PathVariable Long id, @RequestBody KnowledgeFaq faq) {
        faq.setId(id);
        faqMapper.updateById(faq);
        return R.ok(faq);
    }

    @DeleteMapping("/faqs/{id}")
    public R<Void> deleteFaq(@PathVariable Long id) {
        faqMapper.deleteById(id);
        return R.ok(null);
    }

    @GetMapping("/documents")
    public R<PageResp<KnowledgeDocument>> listDocuments(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        IPage<KnowledgeDocument> p = docMapper.selectPage(new Page<>(page, size), null);
        PageResp<KnowledgeDocument> resp = new PageResp<>();
        resp.setRecords(p.getRecords());
        resp.setTotal(p.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        return R.ok(resp);
    }

    @DeleteMapping("/documents/{id}")
    public R<Void> deleteDocument(@PathVariable Long id) {
        docMapper.deleteById(id);
        return R.ok(null);
    }
}
