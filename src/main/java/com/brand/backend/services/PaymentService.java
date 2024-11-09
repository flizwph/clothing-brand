package com.brand.backend.services;

import com.brand.backend.dtos.PaymentDto;
import com.brand.backend.models.Order;
import com.brand.backend.models.Payment;
import com.brand.backend.repositories.OrderRepository;
import com.brand.backend.repositories.PaymentRepository;
import com.brand.backend.integration.payment.PaymentProviderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentProviderFactory paymentProviderFactory;

    @Transactional
    public void createPayment(PaymentDto paymentDto) {
        Order order = orderRepository.findById(paymentDto.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setAmount(order.getPrice());
        payment.setPaymentMethod(paymentDto.getPaymentMethod());
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        // Вызываем провайдер оплаты
        paymentProviderFactory.getPaymentProvider(paymentDto.getPaymentMethod())
                .processPayment(order, payment);
    }
}
