package com.runway.payment;

import com.runway.payment.client.EngagementServiceClient;
import com.runway.payment.client.OrderServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(clients = {OrderServiceClient.class, EngagementServiceClient.class})
public class PaymentServiceApplication {

	public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
