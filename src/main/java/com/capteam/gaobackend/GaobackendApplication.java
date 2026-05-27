package com.capteam.gaobackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
@EnableJpaAuditing
@SpringBootApplication
public class GaobackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GaobackendApplication.class, args);
    }

}
//first commit