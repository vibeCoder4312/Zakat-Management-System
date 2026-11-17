package com.iqra.zakat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ZakatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZakatApplication.class, args);
    }
}
