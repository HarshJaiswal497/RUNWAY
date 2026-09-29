package com.runway.payment.service;

import com.runway.payment.client.OrderServiceClient;
import com.runway.payment.exceptions.ForbiddenException;
import com.runway.payment.exceptions.InvalidPaymentAmountException;
import com.runway.payment.exceptions.InvalidPaymentStatusException;
import com.runway.payment.exceptions.PaymentNotFoundException;
import com.runway.payment.model.PaymentIntent;
import com.runway.payment.repository.PaymentIntentRepo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.runway.payment.repository.PaymentAuditRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @MockitoBean
    private PaymentIntentRepo repository;

    @MockitoBean
    private PaymentAuditRepository paymentAuditRepository;

    @MockitoBean
    private OrderServiceClient orderServiceClient;

    // ── createPaymentIntent – amount validation ───────────────────────────────

    @Test
    void testCreatePaymentIntent_NullAmount_ThrowsInvalidPaymentAmountException() {
        InvalidPaymentAmountException ex = assertThrows(
                InvalidPaymentAmountException.class,
                () -> paymentService.createPaymentIntent(101L, null, 1L)
        );
        assertTrue(ex.getMessage().contains("greater than zero"));
        Mockito.verify(repository, Mockito.never()).save(any());
    }

    @Test
    void testCreatePaymentIntent_ZeroAmount_ThrowsInvalidPaymentAmountException() {
        InvalidPaymentAmountException ex = assertThrows(
                InvalidPaymentAmountException.class,
                () -> paymentService.createPaymentIntent(101L, BigDecimal.ZERO, 1L)
        );
        assertTrue(ex.getMessage().contains("greater than zero"));
        Mockito.verify(repository, Mockito.never()).save(any());
    }

    @Test
    void testCreatePaymentIntent_NegativeAmount_ThrowsInvalidPaymentAmountException() {
        InvalidPaymentAmountException ex = assertThrows(
                InvalidPaymentAmountException.class,
                () -> paymentService.createPaymentIntent(101L, new BigDecimal("-100"), 1L)
        );
        assertTrue(ex.getMessage().contains("greater than zero"));
        Mockito.verify(repository, Mockito.never()).save(any());
    }

    // ── updatePaymentStatus – success cases ──────────────────────────────────

    @Test
    void testUpdatePaymentStatus_Success_Completed() {
        Long orderId = 102L;
        PaymentIntent existingIntent = new PaymentIntent(orderId, new BigDecimal("1000.00"), "PENDING", "order_mock456", 1L);
        Mockito.when(repository.findTopByOrderIdOrderByIdDesc(orderId)).thenReturn(Optional.of(existingIntent));
        Mockito.when(repository.save(any(PaymentIntent.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent result = paymentService.updatePaymentStatus(orderId, "COMPLETED", 1L, "ADMIN");

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        Mockito.verify(repository).findTopByOrderIdOrderByIdDesc(orderId);
        Mockito.verify(repository).save(any(PaymentIntent.class));
        Mockito.verify(orderServiceClient).updateOrderStatusAsAdmin(Mockito.eq(orderId), Mockito.eq("ADMIN"), Mockito.eq(1L), any());
    }

    @Test
    void testUpdatePaymentStatus_Success_Failed() {
        Long orderId = 103L;
        PaymentIntent existingIntent = new PaymentIntent(orderId, new BigDecimal("250.00"), "PENDING", "order_mock789", 1L);
        Mockito.when(repository.findTopByOrderIdOrderByIdDesc(orderId)).thenReturn(Optional.of(existingIntent));
        Mockito.when(repository.save(any(PaymentIntent.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent result = paymentService.updatePaymentStatus(orderId, "FAILED", 1L, "ADMIN");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        Mockito.verify(orderServiceClient, Mockito.never()).updateOrderStatusAsAdmin(
                Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong(), any());
    }

    @Test
    void testUpdatePaymentStatus_StatusIsCaseInsensitive() {
        Long orderId = 105L;
        PaymentIntent existing = new PaymentIntent(orderId, new BigDecimal("500.00"), "PENDING", "order_mock202", 1L);
        Mockito.when(repository.findTopByOrderIdOrderByIdDesc(orderId)).thenReturn(Optional.of(existing));
        Mockito.when(repository.save(any(PaymentIntent.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentIntent result = paymentService.updatePaymentStatus(orderId, "completed", 1L, "ADMIN");

        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void testUpdatePaymentStatus_NonAdmin_ThrowsForbiddenException() {
        Long orderId = 106L;

        ForbiddenException ex = assertThrows(
                ForbiddenException.class,
                () -> paymentService.updatePaymentStatus(orderId, "COMPLETED", 1L, "CUSTOMER")
        );

        assertTrue(ex.getMessage().contains("Only ADMIN users"));
        Mockito.verify(repository, Mockito.never()).findTopByOrderIdOrderByIdDesc(orderId);
    }

    // ── updatePaymentStatus – exception cases ────────────────────────────────

    @Test
    void testUpdatePaymentStatus_NotFound_ThrowsPaymentNotFoundException() {
        Long orderId = 999L;
        Mockito.when(repository.findTopByOrderIdOrderByIdDesc(orderId)).thenReturn(Optional.empty());

        PaymentNotFoundException ex = assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.updatePaymentStatus(orderId, "COMPLETED", 1L, "ADMIN")
        );

        assertTrue(ex.getMessage().contains(String.valueOf(orderId)));
        Mockito.verify(repository, Mockito.never()).save(any());
    }

    @Test
    void testUpdatePaymentStatus_InvalidStatus_ThrowsInvalidPaymentStatusException() {
        Long orderId = 104L;
        PaymentIntent existing = new PaymentIntent(orderId, new BigDecimal("300.00"), "PENDING", "order_mock101", 1L);
        Mockito.when(repository.findTopByOrderIdOrderByIdDesc(orderId)).thenReturn(Optional.of(existing));

        InvalidPaymentStatusException ex = assertThrows(
                InvalidPaymentStatusException.class,
                () -> paymentService.updatePaymentStatus(orderId, "SHIPPED", 1L, "ADMIN")
        );

        assertTrue(ex.getMessage().contains("SHIPPED"));
        Mockito.verify(repository, Mockito.never()).save(any());
    }
}