package com.ner.smartlogix.exception;

/**
 * Thrown when something referenced by id or code does not exist.
 * {@link GlobalExceptionHandler} turns it into a 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** Produces messages like: User not found with id: 42 */
    public ResourceNotFoundException(String resource, String field, Object value) {
        super("%s not found with %s: %s".formatted(resource, field, value));
    }
}
