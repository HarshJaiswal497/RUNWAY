package com.runway.payment.client;

import com.runway.payment.client.dto.ValidateResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthServiceClient {

    @PostMapping("/auth/validate")
    ValidateResponseDTO validateToken(@RequestBody String token);
}
