package com.devarsh.bookstore.controller;

import com.devarsh.bookstore.config.CommonErrors;
import com.devarsh.bookstore.dto.OrderRequest;
import com.devarsh.bookstore.dto.OrderResponse;
import com.devarsh.bookstore.dto.PageResponse;
import com.devarsh.bookstore.dto.StatusUpdateRequest;
import com.devarsh.bookstore.entity.OrderStatus;
import com.devarsh.bookstore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customers place and track orders. Admins see all orders and update their status.")
@SecurityRequirement(name = "bearerAuth")
@CommonErrors
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "List ALL orders, newest first (admin). Optional filter by status.")
    @GetMapping
    public PageResponse<OrderResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "PENDING, SHIPPED, DELIVERED or CANCELLED")
            @RequestParam(required = false) OrderStatus status) {
        return orderService.listAll(page, size, status);
    }

    @Operation(summary = "List my own orders (order history of the logged-in customer)")
    @GetMapping("/my")
    public PageResponse<OrderResponse> listMine(Authentication authentication,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return orderService.listMine(authentication.getName(), page, size);
    }

    @Operation(summary = "Get one order. Customers can only see their own; admins can see any.")
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return orderService.get(id, authentication.getName(), isAdmin);
    }

    @Operation(summary = "Place a new order (customer only). Reduces stock and applies the mock payment.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@Valid @RequestBody OrderRequest request, Authentication authentication) {
        return orderService.place(request, authentication.getName());
    }

    @Operation(summary = "Update order status (admin only)")
    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return orderService.updateStatus(id, request.status());
    }
}
