package com.devarsh.bookstore.exception;

/** Thrown when the request is valid JSON but breaks a business rule (HTTP 400). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
