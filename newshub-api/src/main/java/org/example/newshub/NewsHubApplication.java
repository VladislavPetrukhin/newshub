package org.example.newshub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NewsHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewsHubApplication.class, args);

        System.out.println("NewsHub запущен");
        System.out.println("Главная: http://localhost:8080");
        System.out.println("Выбор источников: http://localhost:8080/select-sources");
        System.out.println("Статистика: http://localhost:8080/stats");
    }
}
