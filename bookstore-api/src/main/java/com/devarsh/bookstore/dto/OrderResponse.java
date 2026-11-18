package com.devarsh.bookstore.dto;

import com.devarsh.bookstore.entity.OrderStatus;
import com.devarsh.bookstore.entity.PaymentMethod;
import com.devarsh.bookstore.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        String customerEmail,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        OrderStatus status,
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        LocalDateTime createdAt) {
}
