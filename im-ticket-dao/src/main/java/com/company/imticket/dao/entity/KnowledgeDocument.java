package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("im_knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    private String title;
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private Integer chunkCount;
    private String esIndexName;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public String getEsIndexName() { return esIndexName; }
    public void setEsIndexName(String esIndexName) { this.esIndexName = esIndexName; }
}