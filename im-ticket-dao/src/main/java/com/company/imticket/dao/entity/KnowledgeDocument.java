package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    private String title;

    private String fileName;

    private String fileType;

    private String filePath;

    private Long fileSize;

    private Integer chunkCount;

    private String esIndexName;
}