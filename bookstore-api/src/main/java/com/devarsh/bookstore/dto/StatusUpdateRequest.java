package com.devarsh.bookstore.dto;

import com.devarsh.bookstore.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @Schema(description = "PENDING -> SHIPPED -> DELIVERED, or CANCELLED before delivery", example = "SHIPPED")
        @NotNull(message = "status is required") OrderStatus status) {
}
