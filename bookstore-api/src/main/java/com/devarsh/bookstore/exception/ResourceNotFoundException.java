package com.devarsh.bookstore.exception;

/** Thrown when a book, order or user does not exist (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
