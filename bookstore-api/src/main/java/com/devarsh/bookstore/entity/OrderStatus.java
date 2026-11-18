package com.devarsh.bookstore.entity;

public enum OrderStatus {
    PENDING, SHIPPED, DELIVERED, CANCELLED;

    /** Allowed order life-cycle: PENDING -> SHIPPED -> DELIVERED, and cancel before delivery. */
    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED || next == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
