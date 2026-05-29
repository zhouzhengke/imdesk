package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("im_knowledge_ticket_archive")
public class KnowledgeTicketArchive extends ImmutableBaseEntity {

    private Long ticketId;
    private String ticketNo;
    private String capitalName;
    private String summary;
    private String solution;
    private String keywords;
    private String category;
    private String esDocId;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getCapitalName() { return capitalName; }
    public void setCapitalName(String capitalName) { this.capitalName = capitalName; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getEsDocId() { return esDocId; }
    public void setEsDocId(String esDocId) { this.esDocId = esDocId; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}