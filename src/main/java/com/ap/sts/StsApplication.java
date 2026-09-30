package com.ap.sts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StsApplication {
    public static void main(String[] args) {
        SpringApplication.run(StsApplication.class, args);
    }
}
