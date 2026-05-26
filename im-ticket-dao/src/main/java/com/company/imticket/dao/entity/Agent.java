package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客服表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_agent")
public class Agent extends BaseEntity {

    private String username;

    private String password;

    private String name;

    private String email;

    private String phone;

    private String role;

    private String status;

    private String wecomUserId;

    private String feishuOpenId;
}