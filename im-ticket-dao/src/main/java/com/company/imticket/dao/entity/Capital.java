package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 资方表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_capital")
public class Capital extends BaseEntity {

    private String name;

    private String contactPerson;

    private String contactPhone;

    private LocalDate contractStart;

    private LocalDate contractEnd;

    private Integer status;

    private String remark;
}