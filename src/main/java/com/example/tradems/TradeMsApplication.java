package com.example.tradems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
public class TradeMsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeMsApplication.class, args);
    }

}
