package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库FAQ表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_knowledge_faq")
public class KnowledgeFaq extends BaseEntity {

    private String question;

    private String answer;

    private String keywords;

    private String category;

    private Long hitCount;

    private Integer enabled;
}