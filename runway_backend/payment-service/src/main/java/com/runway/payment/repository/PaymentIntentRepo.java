package com.runway.payment.repository;

import com.runway.payment.model.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentIntentRepo extends JpaRepository<PaymentIntent, Long> {

    Optional<PaymentIntent> findByOrderIdAndStatus(Long orderId, String status);

    Optional<PaymentIntent> findTopByOrderIdOrderByIdDesc(Long orderId);

    Optional<PaymentIntent> findByRazorpayOrderId(String razorpayOrderId); // ← add this
}