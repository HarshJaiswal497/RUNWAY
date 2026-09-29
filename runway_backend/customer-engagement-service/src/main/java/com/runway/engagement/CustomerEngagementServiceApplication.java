package com.runway.engagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.runway.engagement.client")
public class CustomerEngagementServiceApplication {
	public static void main(String[] args) {
        SpringApplication.run(CustomerEngagementServiceApplication.class, args);
	}
}
