package com.devarsh.bookstore.exception;

/** Thrown when the request clashes with existing data, e.g. duplicate email or ISBN (HTTP 409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
