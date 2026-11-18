package com.devarsh.bookstore.dto;

import java.math.BigDecimal;

public record OrderItemResponse(Long bookId, String title, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
}
