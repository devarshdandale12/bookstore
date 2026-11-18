package com.devarsh.bookstore.service;

import com.devarsh.bookstore.dto.*;
import com.devarsh.bookstore.entity.*;
import com.devarsh.bookstore.exception.BadRequestException;
import com.devarsh.bookstore.exception.ResourceNotFoundException;
import com.devarsh.bookstore.repository.BookRepository;
import com.devarsh.bookstore.repository.OrderRepository;
import com.devarsh.bookstore.repository.UserRepository;
import com.devarsh.bookstore.util.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orders;
    private final BookRepository books;
    private final UserRepository users;

    /**
     * Places an order. Everything happens in ONE transaction: if any book is out of stock,
     * nothing is saved and no stock is reduced.
     */
    @Transactional
    public OrderResponse place(OrderRequest req, String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("This account no longer exists"));

        // If the same book appears twice, add the quantities together.
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (OrderItemRequest item : req.items()) {
            wanted.merge(item.bookId(), item.quantity(), Integer::sum);
        }

        Order order = new Order();
        order.setUser(user);
        order.setPaymentMethod(req.paymentMethod());
        BigDecimal total = BigDecimal.ZERO;

        // Lock books in id order so two orders can never wait on each other (deadlock).
        for (Long bookId : new TreeSet<>(wanted.keySet())) {
            int qty = wanted.get(bookId);
            Book book = books.findByIdForUpdate(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + bookId));
            if (book.getStockQuantity() < qty) {
                throw new BadRequestException("Not enough stock for '" + book.getTitle()
                        + "'. Requested " + qty + ", available " + book.getStockQuantity());
            }
            book.setStockQuantity(book.getStockQuantity() - qty);

            OrderItem item = new OrderItem();
            item.setBook(book);
            item.setQuantity(qty);
            item.setUnitPrice(book.getPrice());
            order.addItem(item);
            total = total.add(item.lineTotal());
        }
        order.setTotalAmount(total);

        // Mock payment: card and UPI are "paid" instantly, cash on delivery is paid later.
        order.setPaymentStatus(req.paymentMethod() == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.PAID);

        return toResponse(orders.save(order));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listAll(int page, int size, OrderStatus status) {
        var pageable = Paging.of(page, size, Sort.by("id").descending());
        Page<Order> result = status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listMine(String email, int page, int size) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("This account no longer exists"));
        var pageable = Paging.of(page, size, Sort.by("id").descending());
        return PageResponse.from(orders.findByUserId(user.getId(), pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id, String email, boolean isAdmin) {
        Order order = find(id);
        if (!isAdmin && !order.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You can only view your own orders");
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus next) {
        Order order = find(id);
        if (!order.getStatus().canMoveTo(next)) {
            throw new BadRequestException("Cannot change order status from " + order.getStatus() + " to " + next);
        }
        if (next == OrderStatus.CANCELLED) {
            // Put the books back on the shelf.
            for (OrderItem item : order.getItems()) {
                Book book = item.getBook();
                book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        }
        if (next == OrderStatus.DELIVERED && order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.PAID);   // cash collected on delivery
        }
        order.setStatus(next);
        return toResponse(order);
    }

    private Order find(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
    }

    private OrderResponse toResponse(Order o) {
        var items = o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getBook().getId(), i.getBook().getTitle(),
                        i.getQuantity(), i.getUnitPrice(), i.lineTotal()))
                .toList();
        return new OrderResponse(o.getId(), o.getUser().getName(), o.getUser().getEmail(), items,
                o.getTotalAmount(), o.getStatus(), o.getPaymentStatus(), o.getPaymentMethod(), o.getCreatedAt());
    }
}
