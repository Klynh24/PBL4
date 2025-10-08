package com.example.signaling;

import com.example.signaling.config.SignalingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SignalingProperties.class)
public class SignalingServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignalingServerApplication.class, args);
    }
}
