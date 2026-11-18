package com.devarsh.bookstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "admin@bookstore.com") @NotBlank(message = "Email is required") String email,
        @Schema(example = "Admin@123") @NotBlank(message = "Password is required") String password) {
}
