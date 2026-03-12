package com.cloudprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CloudProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudProcessingApplication.class, args);
    }
}
