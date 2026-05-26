package com.company.imticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.company.imticket")
@MapperScan("com.company.imticket.dao.mapper")
public class ImTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImTicketApplication.class, args);
    }
}