package com.example.maplesunday;

import com.example.maplesunday.service.NoticeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MapleSundayApplication {
    public static void main(String[] args) {
        var context = SpringApplication.run(MapleSundayApplication.class, args);
        System.exit(SpringApplication.exit(context));
    }
    @Bean CommandLineRunner runOnce(NoticeService service) {
        return args -> service.checkOnce();
    }
}
