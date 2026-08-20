package com.learntrix.edtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EdtechApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdtechApplication.class, args);
    }
}
