package com.javaclimb.chillchat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.servlet.MultipartConfigElement;

@SpringBootApplication(scanBasePackages = {"com.javaclimb.chillchat"})
@MapperScan(basePackages = {"com.javaclimb.chillchat.mappers"})
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class ChillChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChillChatApplication.class, args);
    }

}
