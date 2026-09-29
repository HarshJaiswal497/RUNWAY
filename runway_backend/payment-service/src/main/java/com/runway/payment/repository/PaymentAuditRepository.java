package com.runway.payment.repository;

import com.runway.payment.model.PaymentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAuditRepository extends JpaRepository<PaymentAudit, Long> {

    // Find all audit records for a specific payment intent
    List<PaymentAudit> findByPaymentIntentIdOrderByCreatedAtDesc(Long paymentIntentId);

    // Find audit records by action type
    List<PaymentAudit> findByActionOrderByCreatedAtDesc(String action);
}

