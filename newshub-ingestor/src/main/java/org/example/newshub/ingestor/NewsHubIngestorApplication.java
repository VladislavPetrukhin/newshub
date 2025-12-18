package org.example.newshub.ingestor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsHubIngestorApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewsHubIngestorApplication.class, args);
        System.out.println("NewsHub Ingestor запущен");
        System.out.println("Internal refresh endpoint: http://localhost:8081/internal/refresh");
    }
}
