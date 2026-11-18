package com.devarsh.bookstore.dto;

import java.math.BigDecimal;

public record BookResponse(
        Long id,
        String title,
        String author,
        String genre,
        String isbn,
        BigDecimal price,
        String description,
        int stockQuantity,
        String imageUrl,
        Double averageRating,
        long reviewCount) {
}
