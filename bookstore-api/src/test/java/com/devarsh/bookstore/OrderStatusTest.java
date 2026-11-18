package com.devarsh.bookstore;

import com.devarsh.bookstore.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain unit test (no Spring, no database): the order status rules. */
class OrderStatusTest {

    @Test
    void allowedMoves() {
        assertTrue(OrderStatus.PENDING.canMoveTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.PENDING.canMoveTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.SHIPPED.canMoveTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.SHIPPED.canMoveTo(OrderStatus.CANCELLED));
    }

    @Test
    void forbiddenMoves() {
        assertFalse(OrderStatus.PENDING.canMoveTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.DELIVERED.canMoveTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.CANCELLED.canMoveTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.SHIPPED.canMoveTo(OrderStatus.PENDING));
    }
}
