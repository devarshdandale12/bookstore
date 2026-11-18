package com.devarsh.bookstore.controller;

import com.devarsh.bookstore.config.CommonErrors;
import com.devarsh.bookstore.dto.BookRequest;
import com.devarsh.bookstore.dto.BookResponse;
import com.devarsh.bookstore.dto.PageResponse;
import com.devarsh.bookstore.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Browse books (public). Create, update and delete are admin only.")
@CommonErrors
public class BookController {

    private final BookService bookService;

    @Operation(summary = "List books with pagination, search (title/author), genre filter and sorting")
    @GetMapping
    public PageResponse<BookResponse> list(
            @Parameter(description = "Page number, starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page (1-100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Matches title or author") @RequestParam(required = false) String search,
            @Parameter(description = "Exact genre, e.g. Fiction") @RequestParam(required = false) String genre,
            @Parameter(description = "id, title, author, genre, price, stockQuantity or createdAt")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "asc") String direction) {
        return bookService.list(page, size, search, genre, sortBy, direction);
    }

    @Operation(summary = "List all genres that have at least one book")
    @GetMapping("/genres")
    public List<String> genres() {
        return bookService.genres();
    }

    @Operation(summary = "Get one book by id")
    @GetMapping("/{id}")
    public BookResponse get(@PathVariable Long id) {
        return bookService.get(id);
    }

    @Operation(summary = "Add a new book (admin)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@Valid @RequestBody BookRequest request) {
        return bookService.create(request);
    }

    @Operation(summary = "Update a book, e.g. price or stock (admin)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return bookService.update(id, request);
    }

    @Operation(summary = "Delete a book (admin). Books that are part of an order cannot be deleted.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        bookService.delete(id);
        return Map.of("message", "Book deleted successfully");
    }
}
