package com.ccc.zerocodegenerateproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZeroCodeGenerateProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZeroCodeGenerateProjectApplication.class, args);
    }

}
