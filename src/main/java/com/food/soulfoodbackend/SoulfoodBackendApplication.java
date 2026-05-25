package com.food.soulfoodbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SoulfoodBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoulfoodBackendApplication.class, args);
    }

}
