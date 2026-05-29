package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("im_knowledge_faq")
public class KnowledgeFaq extends BaseEntity {

    private String question;
    private String answer;
    private String keywords;
    private String category;
    private Long hitCount;
    private Integer enabled;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getHitCount() { return hitCount; }
    public void setHitCount(Long hitCount) { this.hitCount = hitCount; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}