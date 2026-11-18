package com.devarsh.bookstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @Schema(example = "5") @Min(value = 1, message = "Rating must be between 1 and 5") @Max(value = 5, message = "Rating must be between 1 and 5") int rating,
        @Schema(example = "Changed how I write code.") @Size(max = 1000, message = "Comment must be at most 1000 characters") String comment) {
}
