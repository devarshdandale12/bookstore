package com.devarsh.bookstore.controller;

import com.devarsh.bookstore.config.CommonErrors;
import com.devarsh.bookstore.dto.ReviewRequest;
import com.devarsh.bookstore.dto.ReviewResponse;
import com.devarsh.bookstore.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books/{bookId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Optional enhancement: ratings and comments on books")
@CommonErrors
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "List reviews of a book (public)")
    @GetMapping
    public List<ReviewResponse> list(@PathVariable Long bookId) {
        return reviewService.list(bookId);
    }

    @Operation(summary = "Rate and review a book. Posting again updates your review.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse addOrUpdate(@PathVariable Long bookId,
                                      @Valid @RequestBody ReviewRequest request,
                                      Authentication authentication) {
        return reviewService.addOrUpdate(bookId, request, authentication.getName());
    }
}
