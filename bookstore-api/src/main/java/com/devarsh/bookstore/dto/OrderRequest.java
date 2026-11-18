package com.devarsh.bookstore.dto;

import com.devarsh.bookstore.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "An order needs at least one item") List<@Valid OrderItemRequest> items,

        @Schema(description = "CARD and UPI are mock-paid immediately; COD stays PENDING until delivery", example = "CARD")
        @NotNull(message = "paymentMethod is required (CARD, UPI or COD)") PaymentMethod paymentMethod) {
}
