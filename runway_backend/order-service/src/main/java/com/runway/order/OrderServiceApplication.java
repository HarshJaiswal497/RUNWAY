package com.runway.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("""
                \n
                ╔══════════════════════════════════════════════════════╗
                ║         ORDER SERVICE started successfully           ║
                ║   Port    : 2000                                     ║
                ║   Swagger : http://localhost:2000/swagger-ui.html    ║
                ║   Health  : http://localhost:2000/actuator/health    ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}
