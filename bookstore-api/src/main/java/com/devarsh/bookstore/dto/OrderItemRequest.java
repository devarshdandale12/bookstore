package com.devarsh.bookstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @Schema(example = "1") @NotNull(message = "bookId is required") Long bookId,
        @Schema(example = "2") @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
}
