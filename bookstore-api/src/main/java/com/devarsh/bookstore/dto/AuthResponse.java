package com.devarsh.bookstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "JWT - send it as: Authorization: Bearer <token>") String token,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Token lifetime in milliseconds", example = "86400000") long expiresInMs,
        UserResponse user) {
}
