package com.devarsh.bookstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BookRequest(
        @Schema(example = "Clean Code")
        @NotBlank(message = "Title is required") @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Schema(description = "One or more authors separated by commas", example = "Robert C. Martin")
        @NotBlank(message = "Author is required") @Size(max = 200, message = "Author must be at most 200 characters")
        String author,

        @Schema(example = "Programming")
        @NotBlank(message = "Genre is required") @Size(max = 80, message = "Genre must be at most 80 characters")
        String genre,

        @Schema(example = "9780132350884")
        @NotBlank(message = "ISBN is required") @Size(max = 20, message = "ISBN must be at most 20 characters")
        String isbn,

        @Schema(example = "599.00")
        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Price can have at most 2 decimal places")
        BigDecimal price,

        @Schema(example = "A handbook of agile software craftsmanship.")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Schema(example = "25")
        @NotNull(message = "Stock quantity is required") @Min(value = 0, message = "Stock cannot be negative")
        Integer stockQuantity,

        @Schema(description = "Cover image URL", example = "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg")
        @Size(max = 500, message = "Image URL must be at most 500 characters")
        String imageUrl) {
}
