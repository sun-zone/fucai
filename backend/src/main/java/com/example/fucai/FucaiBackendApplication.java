package com.example.fucai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FucaiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FucaiBackendApplication.class, args);
    }
}
