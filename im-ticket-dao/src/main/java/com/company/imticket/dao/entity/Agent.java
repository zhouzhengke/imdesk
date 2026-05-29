package com.company.imticket.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;

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

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWecomUserId() { return wecomUserId; }
    public void setWecomUserId(String wecomUserId) { this.wecomUserId = wecomUserId; }
    public String getFeishuOpenId() { return feishuOpenId; }
    public void setFeishuOpenId(String feishuOpenId) { this.feishuOpenId = feishuOpenId; }
}