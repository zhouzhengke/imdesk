package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 历史工单归档表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
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
}