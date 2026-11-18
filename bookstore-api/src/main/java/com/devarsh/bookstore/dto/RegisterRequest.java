package com.devarsh.bookstore.dto;

import com.devarsh.bookstore.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "Devarsh Patel")
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Schema(example = "devarsh@example.com")
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address")
        String email,

        @Schema(example = "secret123")
        @NotBlank(message = "Password is required") @Size(min = 6, max = 72, message = "Password must be 6 to 72 characters")
        String password,

        @Schema(description = "CUSTOMER (default) or ADMIN", example = "CUSTOMER")
        Role role,

        @Schema(description = "Required only when role is ADMIN", example = "BOOKSTORE-ADMIN-2026")
        String adminCode) {
}
