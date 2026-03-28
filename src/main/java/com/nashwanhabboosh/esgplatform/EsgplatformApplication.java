package com.nashwanhabboosh.esgplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EsgplatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(EsgplatformApplication.class, args);
    }
}